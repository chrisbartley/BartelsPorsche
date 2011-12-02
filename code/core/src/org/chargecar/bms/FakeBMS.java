package org.chargecar.bms;

import org.chargecar.serial.streaming.FakeStreamingSerialDevice;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
class FakeBMS extends FakeStreamingSerialDevice
   {
   FakeBMS()
      {
      super(FakeBMS.class.getResourceAsStream("/org/chargecar/bms/bartels-bms.bin"));
      }
   }
