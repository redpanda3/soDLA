// See LICENSE.txt for license details.
package nvdla

import chisel3._
import chisel3.iotesters.{Driver, TesterOptionsManager}
import utils.TutorialRunner

object cmacSINTLauncher {  
  implicit val conf: cmacSINTConfiguration = new cmacSINTConfiguration
  val cmac = Map(
      "NV_NVDLA_CMAC_CORE_macSINT" -> { 
        (manager: TesterOptionsManager) =>
        Driver.execute(() => new NV_NVDLA_CMAC_CORE_macSINT(), manager) {
          (c) => new NV_NVDLA_CMAC_CORE_macSINTTests(c)
        }
      }    

  )
  def main(args: Array[String]): Unit = {
    TutorialRunner("cmac", cmac, args)
  }
}

object cmacLauncher {  
  implicit val conf: cmacConfiguration = new cmacConfiguration
  val cmac = Map(
      "NV_NVDLA_CMAC_CORE_mac" -> { 
        (manager: TesterOptionsManager) =>
        Driver.execute(() => new NV_NVDLA_CMAC_CORE_mac(), manager) {
          (c) => new NV_NVDLA_CMAC_CORE_macTests(c)
        }
      }    

  )
  def main(args: Array[String]): Unit = {
    TutorialRunner("cmac", cmac, args)
  }
}

