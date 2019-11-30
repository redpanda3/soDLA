// package cora

// import chisel3._
// import chisel3.experimental._
// import chisel3.util._
// import hardfloat._
// import chisel3.iotesters.Driver

// //this module is to predict p


// class CORA_PRED_p(implicit val conf: matrixConfiguration) extends Module {

//     val io = IO(new Bundle {
//         //input
//         val reg2dp_roundingMode = Input(UInt(3.W))
//         val reg2dp_detectTininess = Input(Bool())
//         val reg2dp_noise_ax2 = Input(UInt(conf.KF_BPE.W))
//         val reg2dp_noise_ay2 = Input(UInt(conf.KF_BPE.W))

//         val pre_p_st = Input(Bool()) //st is only for one cycle
//         val pre_p_done = Output(Bool())

//         val tr_f_actv_data = Input(Vec(4, Vec(4, UInt(conf.KF_BPE.W))))
//         val tr_f_actv_pvld = Input(Bool())

//         val tr_p_actv_data = Input(Vec(4, Vec(4, UInt(conf.KF_BPE.W))))
//         val tr_p_actv_pvld = Input(Bool())

//         val dt_actv_data = Input(UInt(conf.KF_BPE.W))
//         val dt_actv_pvld = Input(Bool())

//         //output
//         val tr_p_data = Output(Vec(4, Vec(4, UInt(conf.KF_BPE.W))))
//         val tr_p_pvld = Output(Bool())

//     })

// //     
// //          ┌─┐       ┌─┐
// //       ┌──┘ ┴───────┘ ┴──┐
// //       │                 │
// //       │       ───       │
// //       │  ─┬┘       └┬─  │
// //       │                 │
// //       │       ─┴─       │                            need 12 pipes to finish
// //       │                 │
// //       └───┐         ┌───┘                           |0  |  2  |  4  |  6  |  8  |  10  |  12  |  14  |  16  |
// //           │         │                                     
// //           │         │                               
// //           │         │                               |-----> m2m2m -------------------> | 
// //           │         └──────────────┐                                                   |add-->| result
// //           │                        │                |dt | dt2 | dt4 |dt4/4|*ay  | *ay  |
// //                                                              |  dt3 |dt3/2|*ax  | *ax  |
// //           │                        ├─┐                   
// //           │                        ┌─┘              
// //           │                        │                            
// //           └─┐  ┐  ┌───────┬──┐  ┌──┘                           
// //             │ ─┤ ─┤       │ ─┤ ─┤            
// //             └──┴──┘       └──┴──┘ 


//     //Hardware Reuse
//     val u_transpose = Module(new CORA_MATRIX_transpose)
//     u_transpose.io.tr_actv_data := io.tr_f_actv_data
//     u_transpose.io.tr_actv_pvld := io.tr_f_actv_pvld

//     val tr_f_transpose_out_data = u_transpose.io.transpose_out_data
//     val tr_f_transpose_out_pvld = u_transpose.io.transpose_out_pvld

//     //clock counter
//     //one pipe to receive start signal    

//     val clk_cnt = RegInit(0.U)
//     clk_cnt := Mux(io.pre_p_st, 0.U,
//                Mux(io.pre_p_done, 0.U,
//                clk_cnt + 1.U))
    
//     io.pre_p_done := ( clk_cnt === (7*conf.HARDFLOAT_MAC_LATENCY).U)

    
//     //setup pipelines
//     //calculate f, p, ft
//     val din_pvld_first_stage = io.tr_f_actv_pvld & io.tr_p_actv_pvld & io.dt_actv_pvld & io.pre_p_st

//     // each variables in each stages
//     val dout_pvld_first_stage = RegInit(false.B)
//     val dt2_first_stage = Reg(UInt((conf.KF_BPE).W))

//     val dout_pvld_second_stage = RegInit(false.B)
//     val dt3_second_stage = Reg(UInt((conf.KF_BPE).W))
//     val dt4_second_stage = Reg(UInt((conf.KF_BPE).W))

//     val dout_pvld_third_stage = RegInit(false.B)
//     val dt3_2_third_stage = Reg(UInt((conf.KF_BPE).W))
//     val dt4_4_third_stage = Reg(UInt((conf.KF_BPE).W))

//     val dout_pvld_fourth_stage = RegInit(false.B)
//     val dt3_2_ax_fourth_stage = Reg(UInt((conf.KF_BPE).W))
//     val dt4_4_ax_fourth_stage = Reg(UInt((conf.KF_BPE).W))

//     val dout_pvld_fifth_stage = RegInit(false.B)
//     val dt3_2_ay_fourth_stage = Reg(UInt((conf.KF_BPE).W))
//     val dt4_4_ay_fourth_stage = Reg(UInt((conf.KF_BPE).W))

//     val dout_pvld_sixth_stage = RegInit(false.B)
//     val q_sixth_stage = Reg(Vec(4, Vec(4, UInt((conf.KF_BPE).W))))

//     //set up modules
//     val u_mac = Array.fill(2)(Module(new MulAddRecFNPipe())) 
//     val u_m2m2m = Module(new CORA_MATRIX_MUL_m2m2m)
//     val u_madd = Module(new CORA_MATRIX_ADD_m2m)

//     //setup config
//     u_m2m2m.io.reg2dp_roundingMode := io.reg2dp_roundingMode
//     u_m2m2m.io.reg2dp_detectTininess := io.reg2dp_detectTininess   

//     u_madd.io.reg2dp_roundingMode := io.reg2dp_roundingMode
//     u_madd.io.reg2dp_detectTininess := io.reg2dp_detectTininess

//     for(i <- 0 to 1){
//         u_mac(i).io.reg2dp_roundingMode := io.reg2dp_roundingMode
//         u_mac(i).io.reg2dp_detectTininess := io.reg2dp_detectTininess
//     }

//     when((clk_cnt >= 0.U) & (clk_cnt <= (conf.HARDFLOAT_MAC_LATENCY-1).U)){
//         //m2m2m 
//         u_m2m2m.io.st := din_pvld_first_stage
//         u_m2m2m.io.tr_a_actv_data := io.tr_f_actv_data
//         u_m2m2m.io.tr_a_actv_pvld := din_pvld_first_stage

//         u_m2m2m.io.tr_b_actv_data := io.tr_p_actv_data
//         u_m2m2m.io.tr_b_actv_pvld := din_pvld_first_stage

//         u_m2m2m.io.tr_c_actv_data := tr_f_transpose_out_data
//         u_m2m2m.io.tr_c_actv_pvld := din_pvld_first_stage

//         //m2m add
//         u_madd.io.tr_a_actv_data := VecInit(Seq.fill(4)(VecInit(Seq.fill(4)("b0".asUInt((conf.KF_BPE).W)))))
//         u_madd.io.tr_a_actv_pvld := false.B

//         u_madd.io.tr_b_actv_data := VecInit(Seq.fill(4)(VecInit(Seq.fill(4)("b0".asUInt((conf.KF_BPE).W)))))
//         u_madd.io.tr_b_actv_pvld := false.B

//         //multiply 
//         u_mac(0).io.validin := din_pvld_first_stage
//         u_mac(0).io.a := io.dt_actv_data
//         u_mac(0).io.b := io.dt_actv_data
//         u_mac(0).io.c := "b0".asUInt(conf.KF_BPE.W)

//         u_mac(1).io.validin := din_pvld_first_stage
//         u_mac(1).io.a := "b0".asUInt(conf.KF_BPE.W)  
//         u_mac(1).io.b := "b0".asUInt(conf.KF_BPE.W)     
//         u_mac(1).io.c := "b0".asUInt(conf.KF_BPE.W)  
      
//     }
//     .elsewhen((clk_cnt >= (conf.HARDFLOAT_MAC_LATENCY).U) & (clk_cnt <= (2*conf.HARDFLOAT_MAC_LATENCY-1).U)){
//         //result from first stage
//         dout_pvld_first_stage := u_mac(0).io.validout
//         dt2_first_stage := u_mac(0).io.out

//         //m2m2m 
//         u_m2m2m.io.st := dout_pvld_first_stage
//         u_m2m2m.io.tr_a_actv_data := io.tr_f_actv_data
//         u_m2m2m.io.tr_a_actv_pvld := din_pvld_first_stage

//         u_m2m2m.io.tr_b_actv_data := io.tr_p_actv_data
//         u_m2m2m.io.tr_b_actv_pvld := din_pvld_first_stage

//         u_m2m2m.io.tr_c_actv_data := tr_f_transpose_out_data
//         u_m2m2m.io.tr_c_actv_pvld := din_pvld_first_stage

//         //m2m add
//         u_madd.io.tr_a_actv_data := VecInit(Seq.fill(4)(VecInit(Seq.fill(4)("b0".asUInt((conf.KF_BPE).W)))))
//         u_madd.io.tr_a_actv_pvld := false.B

//         u_madd.io.tr_b_actv_data := VecInit(Seq.fill(4)(VecInit(Seq.fill(4)("b0".asUInt((conf.KF_BPE).W)))))
//         u_madd.io.tr_b_actv_pvld := false.B

//         //multiply 
//         u_mac(0).io.validin := dout_pvld_first_stage
//         u_mac(0).io.a := dt2_first_stage
//         u_mac(0).io.b := dt2_first_stage
//         u_mac(0).io.c := "b0".asUInt(conf.KF_BPE.W)

//         u_mac(1).io.validin := dout_pvld_first_stage
//         u_mac(1).io.a := ShiftRegister(io.dt_actv_data, conf.HARDFLOAT_MAC_LATENCY)
//         u_mac(1).io.b := dt2_first_stage 
//         u_mac(1).io.c := "b0".asUInt(conf.KF_BPE.W)  

//     }
//     .elsewhen((clk_cnt >= (2*conf.HARDFLOAT_MAC_LATENCY).U) & (clk_cnt <= (3*conf.HARDFLOAT_MAC_LATENCY-1).U)){
//         //result from first stage
//         dout_pvld_second_stage := u_mac(0).io.validout
//         dt4_second_stage := u_mac(0).io.out
//         dt3_second_stage := u_mac(1).io.out

//         //m2m2m 
//         u_m2m2m.io.st := dout_pvld_first_stage
//         u_m2m2m.io.tr_a_actv_data := io.tr_f_actv_data
//         u_m2m2m.io.tr_a_actv_pvld := din_pvld_first_stage

//         u_m2m2m.io.tr_b_actv_data := io.tr_p_actv_data
//         u_m2m2m.io.tr_b_actv_pvld := din_pvld_first_stage

//         u_m2m2m.io.tr_c_actv_data := tr_f_transpose_out_data
//         u_m2m2m.io.tr_c_actv_pvld := din_pvld_first_stage

//         //m2m add
//         u_madd.io.tr_a_actv_data := VecInit(Seq.fill(4)(VecInit(Seq.fill(4)("b0".asUInt((conf.KF_BPE).W)))))
//         u_madd.io.tr_a_actv_pvld := false.B

//         u_madd.io.tr_b_actv_data := VecInit(Seq.fill(4)(VecInit(Seq.fill(4)("b0".asUInt((conf.KF_BPE).W)))))
//         u_madd.io.tr_b_actv_pvld := false.B

//         //multiply 
//         u_mac(0).io.validin := dout_pvld_second_stage
//         u_mac(0).io.a := dt4_second_stage
//         u_mac(0).io.b := "b0_001000000_000000000000000000000000".asUInt(conf.KF_BPE.W) //divide by 4
//         u_mac(0).io.c := "b0".asUInt(conf.KF_BPE.W)

//         u_mac(1).io.validin := dout_pvld_second_stage
//         u_mac(1).io.a := dt3_second_stage
//         u_mac(1).io.b := "b0_010000000_000000000000000000000000".asUInt(conf.KF_BPE.W) //divide by 2
//         u_mac(1).io.c := "b0".asUInt(conf.KF_BPE.W)  

//     }
//     .elsewhen((clk_cnt >= (3*conf.HARDFLOAT_MAC_LATENCY).U) & (clk_cnt <= (4*conf.HARDFLOAT_MAC_LATENCY-1).U)){
//         //result from first stage
//         dout_pvld_third_stage := u_mac(0).io.validout
//         dt4_4_fourth_stage := u_mac(0).io.out
//         dt3_2_fourth_stage := u_mac(1).io.out

//         //m2m2m 
//         u_m2m2m.io.st := dout_pvld_third_stage
//         u_m2m2m.io.tr_a_actv_data := io.tr_f_actv_data
//         u_m2m2m.io.tr_a_actv_pvld := din_pvld_first_stage

//         u_m2m2m.io.tr_b_actv_data := io.tr_p_actv_data
//         u_m2m2m.io.tr_b_actv_pvld := din_pvld_first_stage

//         u_m2m2m.io.tr_c_actv_data := tr_f_transpose_out_data
//         u_m2m2m.io.tr_c_actv_pvld := din_pvld_first_stage

//         //m2m add
//         u_madd.io.tr_a_actv_data := VecInit(Seq.fill(4)(VecInit(Seq.fill(4)("b0".asUInt((conf.KF_BPE).W)))))
//         u_madd.io.tr_a_actv_pvld := false.B

//         u_madd.io.tr_b_actv_data := VecInit(Seq.fill(4)(VecInit(Seq.fill(4)("b0".asUInt((conf.KF_BPE).W)))))
//         u_madd.io.tr_b_actv_pvld := false.B

//         //multiply 
//         u_mac(0).io.validin := dout_pvld_third_stage
//         u_mac(0).io.a := dt4_4_third_stage
//         u_mac(0).io.b := io.reg2dp_noise_ax2
//         u_mac(0).io.c := "b0".asUInt(conf.KF_BPE.W)

//         u_mac(1).io.validin := dout_pvld_third_stage
//         u_mac(1).io.a := dt3_2_third_stage
//         u_mac(1).io.b := io.reg2dp_noise_ax2
//         u_mac(1).io.c := "b0".asUInt(conf.KF_BPE.W)  

//     }
//     .elsewhen((clk_cnt >= (4*conf.HARDFLOAT_MAC_LATENCY).U) & (clk_cnt <= (5*conf.HARDFLOAT_MAC_LATENCY-1).U)){
//         //result from first stage
//         dout_pvld_fourth_stage := u_mac(0).io.validout
//         dt4_4_ax_fourth_stage := u_mac(0).io.out
//         dt3_2_ax_fourth_stage := u_mac(1).io.out

//         //m2m2m 
//         u_m2m2m.io.st := dout_pvld_fourth_stage
//         u_m2m2m.io.tr_a_actv_data := io.tr_f_actv_data
//         u_m2m2m.io.tr_a_actv_pvld := din_pvld_first_stage

//         u_m2m2m.io.tr_b_actv_data := io.tr_p_actv_data
//         u_m2m2m.io.tr_b_actv_pvld := din_pvld_first_stage

//         u_m2m2m.io.tr_c_actv_data := tr_f_transpose_out_data
//         u_m2m2m.io.tr_c_actv_pvld := din_pvld_first_stage

//         //m2m add
//         u_madd.io.tr_a_actv_data := VecInit(Seq.fill(4)(VecInit(Seq.fill(4)("b0".asUInt((conf.KF_BPE).W)))))
//         u_madd.io.tr_a_actv_pvld := false.B

//         u_madd.io.tr_b_actv_data := VecInit(Seq.fill(4)(VecInit(Seq.fill(4)("b0".asUInt((conf.KF_BPE).W)))))
//         u_madd.io.tr_b_actv_pvld := false.B

//         //multiply 
//         u_mac(0).io.validin := dout_pvld_fourth_stage
//         u_mac(0).io.a := dt4_4_fourth_stage
//         u_mac(0).io.b := io.reg2dp_noise_ay2
//         u_mac(0).io.c := "b0".asUInt(conf.KF_BPE.W)

//         u_mac(1).io.validin := dout_pvld_fourth_stage
//         u_mac(1).io.a := dt3_2_fourth_stage
//         u_mac(1).io.b := io.reg2dp_noise_ay2
//         u_mac(1).io.c := "b0".asUInt(conf.KF_BPE.W)  

//     }
//     .elsewhen((clk_cnt >= (5*conf.HARDFLOAT_MAC_LATENCY).U) & (clk_cnt <= (6*conf.HARDFLOAT_MAC_LATENCY-1).U)){
//         //result from first stage
//         dout_pvld_fifth_stage := u_mac(0).io.validout
//         dt4_4_ay_fifth_stage := u_mac(0).io.out
//         dt3_2_ay_fifth_stage := u_mac(1).io.out

//         //m2m2m 
//         u_m2m2m.io.st := dout_pvld_fifth_stage
//         u_m2m2m.io.tr_a_actv_data := io.tr_f_actv_data
//         u_m2m2m.io.tr_a_actv_pvld := din_pvld_first_stage

//         u_m2m2m.io.tr_b_actv_data := io.tr_p_actv_data
//         u_m2m2m.io.tr_b_actv_pvld := din_pvld_first_stage

//         u_m2m2m.io.tr_c_actv_data := tr_f_transpose_out_data
//         u_m2m2m.io.tr_c_actv_pvld := din_pvld_first_stage

//         //m2m add
//         u_madd.io.tr_a_actv_data := VecInit(Seq.fill(4)(VecInit(Seq.fill(4)("b0".asUInt((conf.KF_BPE).W)))))
//         u_madd.io.tr_a_actv_pvld := false.B

//         u_madd.io.tr_b_actv_data := VecInit(Seq.fill(4)(VecInit(Seq.fill(4)("b0".asUInt((conf.KF_BPE).W)))))
//         u_madd.io.tr_b_actv_pvld := false.B

//         //multiply 
//         u_mac(0).io.validin := dout_pvld_fifth_stage
//         u_mac(0).io.a := dt4_4_fourth_stage
//         u_mac(0).io.b := io.reg2dp_noise_ay2
//         u_mac(0).io.c := "b0".asUInt(conf.KF_BPE.W)

//         u_mac(1).io.validin := dout_pvld_fifth_stage
//         u_mac(1).io.a := dt3_2_fourth_stage
//         u_mac(1).io.b := io.reg2dp_noise_ay2
//         u_mac(1).io.c := "b0".asUInt(conf.KF_BPE.W)  

//     }
//     .elsewhen((clk_cnt >= (6*conf.HARDFLOAT_MAC_LATENCY).U) & (clk_cnt <= (7*conf.HARDFLOAT_MAC_LATENCY-1).U)){
//         //result from first stage
//         dout_pvld_fifth_stage := u_mac(0).io.validout
//         q_sixth_stage := VecInit(
//                          VecInit(dt4_4_ax_fourth_stage, "b0".asUInt(conf.KF_BPE.W), dt3_2_ax_fourth_stage, "b0".asUInt(conf.KF_BPE.W)),
//                          VecInit("b0".asUInt(conf.KF_BPE.W), "b0".asUInt(conf.KF_BPE.W), dt3_2_ax_fourth_stage, "b0".asUInt(conf.KF_BPE.W)),
//                          VecInit(dt4_4_ax_fourth_stage, "b0".asUInt(conf.KF_BPE.W), dt3_2_ax_fourth_stage, "b0".asUInt(conf.KF_BPE.W)),
//                          VecInit(dt4_4_ax_fourth_stage, "b0".asUInt(conf.KF_BPE.W), dt3_2_ax_fourth_stage, "b0".asUInt(conf.KF_BPE.W)),
//                          )


//         //m2m2m 
//         u_m2m2m.io.st := dout_pvld_fifth_stage
//         u_m2m2m.io.tr_a_actv_data := io.tr_f_actv_data
//         u_m2m2m.io.tr_a_actv_pvld := din_pvld_first_stage

//         u_m2m2m.io.tr_b_actv_data := io.tr_p_actv_data
//         u_m2m2m.io.tr_b_actv_pvld := din_pvld_first_stage

//         u_m2m2m.io.tr_c_actv_data := tr_f_transpose_out_data
//         u_m2m2m.io.tr_c_actv_pvld := din_pvld_first_stage

//         //m2m add
//         u_madd.io.tr_a_actv_data := VecInit(Seq.fill(4)(VecInit(Seq.fill(4)("b0".asUInt((conf.KF_BPE).W)))))
//         u_madd.io.tr_a_actv_pvld := false.B

//         u_madd.io.tr_b_actv_data := VecInit(Seq.fill(4)(VecInit(Seq.fill(4)("b0".asUInt((conf.KF_BPE).W)))))
//         u_madd.io.tr_b_actv_pvld := false.B

//         //multiply 
//         u_mac(0).io.validin := dout_pvld_fifth_stage
//         u_mac(0).io.a := "b0".asUInt(conf.KF_BPE.W)  
//         u_mac(0).io.b := "b0".asUInt(conf.KF_BPE.W)  
//         u_mac(0).io.c := "b0".asUInt(conf.KF_BPE.W)

//         u_mac(1).io.validin := dout_pvld_fifth_stage
//         u_mac(1).io.a := "b0".asUInt(conf.KF_BPE.W)  
//         u_mac(1).io.b := "b0".asUInt(conf.KF_BPE.W)  
//         u_mac(1).io.c := "b0".asUInt(conf.KF_BPE.W)  


//     }
//     .otherwise{
//         //m2m2m zero
//         u_m2m2m.io.tr_a_actv_data := VecInit(Seq.fill(4)(VecInit(Seq.fill(4)("b0".asUInt((conf.KF_BPE).W)))))
//         u_m2m2m.io.tr_a_actv_pvld := false.B

//         u_m2m2m.io.tr_b_actv_data := VecInit(Seq.fill(4)(VecInit(Seq.fill(4)("b0".asUInt((conf.KF_BPE).W)))))
//         u_m2m2m.io.tr_b_actv_pvld := false.B

//         u_m2m2m.io.tr_c_actv_data := VecInit(Seq.fill(4)(VecInit(Seq.fill(4)("b0".asUInt((conf.KF_BPE).W)))))
//         u_m2m2m.io.tr_c_actv_pvld := false.B

//         //v2v add
//         u_madd.io.tr_a_actv_data := VecInit(Seq.fill(4)(VecInit(Seq.fill(4)("b0".asUInt((conf.KF_BPE).W)))))
//         u_madd.io.tr_a_actv_pvld := false.B

//         u_madd.io.tr_b_actv_data := VecInit(Seq.fill(4)(VecInit(Seq.fill(4)("b0".asUInt((conf.KF_BPE).W)))))
//         u_madd.io.tr_b_actv_pvld := false.B

//     }


//     when(io.tr_p_pvld){
//         io.tr_p_data := u_madd.io.tr_out_data
//     }
//     .otherwise{
//         io.tr_p_data := VecInit(Seq.fill(4)(VecInit(Seq.fill(4)("b0".asUInt((conf.KF_BPE).W)))))
//     }
//     io.tr_p_pvld := ShiftRegister(pre_p_st, (2*conf.V2V_MAC_LATENCY + conf.HARDFLOAT_MAC_LATENCY), pre_p_st) &
//                     ShiftRegister(dout_pvld_first_stage, conf.HARDFLOAT_MAC_LATENCY, dout_pvld_first_stage) &
//                     u_madd.io.tr_out_pvld

// }




// object CORA_PRED_pDriver extends App {
//   implicit val conf: matrixConfiguration = new matrixConfiguration
//   chisel3.Driver.execute(args, () => new CORA_PRED_p)
// }