package org.chargecar.bms;

import org.chargecar.serial.streaming.StreamingSerialPortDeviceController;

/**
 * <p>
 * <code>BMSController</code> is the MVC controller class for the {@link BMSModel} and {@link BMSView}.
 * </p>
 *
 * @author Chris Bartley (bartley@cmu.edu)
 */
public final class BMSController extends StreamingSerialPortDeviceController<BMSEvent, BMSAndEnergy>
   {
   private final BMSModel model;

   public static BMSController create(final String serialPortName, final BMSModel model)
      {
      final BMSReader reader;
      final String deviceName;
      if (StreamingSerialPortDeviceController.shouldUseFakeDevice())
         {
         deviceName = "Fake BMS";
         reader = new BMSReader(new FakeBMS(), model.getNumBatteries());
         }
      else
         {
         deviceName = "BMS";
         if (serialPortName == null)
            {
            reader = null;
            }
         else
            {
            reader = new BMSReader(serialPortName, model.getNumBatteries());
            }
         }

      return new BMSController(deviceName, reader, model);
      }

   private BMSController(final String deviceName, final BMSReader reader, final BMSModel model)
      {
      super(deviceName, reader, model);
      this.model = model;
      }

   public void resetBatteryEnergyEquation()
      {
      model.resetBatteryEnergyEquation();
      }

   public void resetBatteryPackCurrentMinMax()
      {
      model.resetBatteryPackCurrentMinMax();
      }

   public void resetBatteryPackVoltageMinMax()
      {
      model.resetBatteryPackVoltageMinMax();
      }
   }
