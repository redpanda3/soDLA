// package nvdla

// import chisel3._
// import chisel3.experimental._
// import chisel3.util._

// class NV_NVDLA_SDP_HLS_X_int_alu extends Module {
//    val LUT_DEPTH = 256
//    val io = IO(new Bundle {
//         val nvdla_core_clk = Input(Clock())

//         val alu_data_in = Input(UInt(32.W))
//         val alu_in_pvld = Input(Bool())
//         val alu_op_pvld = Input(Bool())
//         val alu_out_prdy = Input(Bool())
//         val cfg_alu_algo = Input(UInt(2.W))
//         val cfg_alu_bypass = Input(Bool())
//         val cfg_alu_op = Input(UInt(16.W))
//         val cfg_alu_shift_value = Input(UInt(6.W))
//         val cfg_alu_src = Input(Bool())
//         val chn_alu_op = Input(UInt(16.W))

//         val alu_data_out = Output(UInt(33.W))
//         val alu_in_prdy = Output(Bool())
//         val alu_op_prdy = Output(Bool())
//         val alu_out_pvld = Output(Bool())
//     })
//     //     
//     //          ┌─┐       ┌─┐
//     //       ┌──┘ ┴───────┘ ┴──┐
//     //       │                 │
//     //       │       ───       │          
//     //       │  ─┬┘       └┬─  │
//     //       │                 │
//     //       │       ─┴─       │
//     //       │                 │
//     //       └───┐         ┌───┘
//     //           │         │
//     //           │         │
//     //           │         │
//     //           │         └──────────────┐
//     //           │                        │
//     //           │                        ├─┐
//     //           │                        ┌─┘    
//     //           │                        │
//     //           └─┐  ┐  ┌───────┬──┐  ┌──┘         
//     //             │ ─┤ ─┤       │ ─┤ ─┤         
//     //             └──┴──┘       └──┴──┘ 
// withClock(io.nvdla_core_clk){

//     val alu_sync_prdy = Wire(Bool())

//     val x_alu_sync2data = Module{new NV_NVDLA_SDP_HLS_sync2data}
//     x_alu_sync2data.io.chn1_en := io.cfg_alu_src & !io.cfg_alu_bypass
//     x_alu_sync2data.io.chn2_en := !io.cfg_alu_bypass
//     x_alu_sync2data.io.chn1_in_pvld := io.alu_op_pvld
//     io.alu_op_prdy := x_alu_sync2data.io.chn1_in_prdy
//     x_alu_sync2data.io.chn2_in_pvld := io.alu_in_pvld
//     val alu_in_srdy = x_alu_sync2data.io.chn2_in_prdy
//     val alu_sync_pvld = x_alu_sync2data.io.chn_out_pvld    
//     x_alu_sync2data.io.chn_out_prdy := alu_sync_prdy        
//     io.chn_alu_op := x_alu_sync2data.io.data1_in 
//     io.alu_data_in :=  x_alu_sync2data.io.data2_in
//     val alu_op_sync = x_alu_sync2data.io.data1_out
//     val alu_data_sync = x_alu_sync2data.io.data2_out

//     val alu_op_in = Mux(io.cfg_alu_src, alu_op_sync, io.cfg_alu_op)

//     val x_alu_shiftleft_su = Module{new NV_NVDLA_HLS_shiftleftsu(16, 32, 6)}
//     x_alu_shiftleft_su.io.data_in := alu_op_in
//     x_alu_shiftleft_su.io.shift_num := io.cfg_alu_shift_value
//     val alu_op_shift = x_alu_shiftleft_su.io.data_out

//     val pipe_p1 = Module{new NV_NVDLA_SDP_HLS_X_INT_ALU_pipe_p1}














// }}
