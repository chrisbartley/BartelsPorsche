package org.chargecar.bms;

import org.apache.log4j.Logger;
import org.chargecar.serial.streaming.StreamingSerialPortDeviceModel;

/**
 * <p>
 * <code>BMSModel</code> keeps track of BMS data.
 * </p>
 *
 * @author Chris Bartley (bartley@cmu.edu)
 */
public final class BMSModel extends StreamingSerialPortDeviceModel<BMSEvent, BMSAndEnergy>
   {
   private static final Logger LOG = Logger.getLogger(BMSModel.class);
   private static final Logger DATA_LOG = Logger.getLogger("DataLog");

   public static final double HOURS_PER_MILLISECOND = 1 / 3600000.0;
   private static final double MILLIS_TO_KILOHOURS_MULTIPLIER = HOURS_PER_MILLISECOND / 1000;

   private final int numBatteries;
   private final byte[] dataSynchronizationLock = new byte[0];
   private BMSEvent previousBMSEvent = null;
   private final EnergyEquationImpl batteryEnergyEquation = new EnergyEquationImpl();
   private final UpdatableDouble minBatteryPackVoltage = UpdatableDouble.createMinimumDouble(null);
   private final UpdatableDouble maxBatteryPackVoltage = UpdatableDouble.createMaximumDouble(null);
   private final UpdatableDouble minBatteryPackCurrent = UpdatableDouble.createMinimumDouble(null);
   private final UpdatableDouble maxBatteryPackCurrent = UpdatableDouble.createMaximumDouble(null);

   public BMSModel(final int numBatteries)
      {
      this.numBatteries = numBatteries;
      }

   public int getNumBatteries()
      {
      return numBatteries;
      }

   public BMSAndEnergy update(final BMSEvent bmsEvent)
      {
      synchronized (dataSynchronizationLock)
         {
         // if the previous BMSEvent isn't null, then calculate the energy change
         if (previousBMSEvent != null)
            {
            final long elapsedMilliseconds = bmsEvent.getTimestampMilliseconds() - previousBMSEvent.getTimestampMilliseconds();
            final double elapsedKiloHours = elapsedMilliseconds * MILLIS_TO_KILOHOURS_MULTIPLIER;
            final double batteryKwh = bmsEvent.getPackTotalVoltage() *
                                      bmsEvent.getLoadCurrentAmps() *
                                      elapsedKiloHours;

            batteryEnergyEquation.addKilowattHours(batteryKwh);
            }

         // save the BMSEvent
         previousBMSEvent = bmsEvent;

         minBatteryPackVoltage.update(bmsEvent.getPackTotalVoltage());
         maxBatteryPackVoltage.update(bmsEvent.getPackTotalVoltage());
         minBatteryPackCurrent.update(bmsEvent.getLoadCurrentAmps());
         maxBatteryPackCurrent.update(bmsEvent.getLoadCurrentAmps());

         final BMSAndEnergyImpl bmsAndEnergy = new BMSAndEnergyImpl(bmsEvent,
                                                                    batteryEnergyEquation,
                                                                    minBatteryPackVoltage.getValue(),
                                                                    maxBatteryPackVoltage.getValue(),
                                                                    minBatteryPackCurrent.getValue(),
                                                                    maxBatteryPackCurrent.getValue());

         if (DATA_LOG.isInfoEnabled())
            {
            DATA_LOG.info(bmsEvent.toLoggingString());
            DATA_LOG.info(batteryEnergyEquation.toLoggingString(bmsEvent.getTimestampMilliseconds()));
            }

         publishEventToListeners(bmsAndEnergy);

         return bmsAndEnergy;
         }
      }

   public void resetBatteryEnergyEquation()
      {
      synchronized (dataSynchronizationLock)
         {
         batteryEnergyEquation.reset();
         }
      }

   public void resetBatteryPackCurrentMinMax()
      {
      synchronized (dataSynchronizationLock)
         {
         minBatteryPackCurrent.setValue(previousBMSEvent.getLoadCurrentAmps());
         maxBatteryPackCurrent.setValue(previousBMSEvent.getLoadCurrentAmps());
         }
      }

   public void resetBatteryPackVoltageMinMax()
      {
      synchronized (dataSynchronizationLock)
         {
         minBatteryPackVoltage.setValue(previousBMSEvent.getPackTotalVoltage());
         maxBatteryPackVoltage.setValue(previousBMSEvent.getPackTotalVoltage());
         }
      }

   private static final class BMSAndEnergyImpl implements BMSAndEnergy
      {
      private final BMSEvent bmsEvent;
      private final EnergyEquation energyEquation;
      private final Double minBatteryPackVoltage;
      private final Double minBatteryPackCurrent;
      private final Double maxBatteryPackCurrent;
      private final Double maxBatteryPackVoltage;

      private BMSAndEnergyImpl(final BMSEvent bmsEvent,
                               final EnergyEquation energyEquation,
                               final Double minBatteryPackVoltage,
                               final Double maxBatteryPackVoltage,
                               final Double minBatteryPackCurrent,
                               final Double maxBatteryPackCurrent)
         {
         this.bmsEvent = bmsEvent;
         this.energyEquation = energyEquation;
         this.minBatteryPackVoltage = minBatteryPackVoltage;
         this.maxBatteryPackVoltage = maxBatteryPackVoltage;
         this.minBatteryPackCurrent = minBatteryPackCurrent;
         this.maxBatteryPackCurrent = maxBatteryPackCurrent;
         }

      @Override
      public BMSEvent getBmsState()
         {
         return bmsEvent;
         }

      @Override
      public EnergyEquation getEnergyEquation()
         {
         return energyEquation;
         }

      @Override
      public Double getMinBatteryPackVoltage()
         {
         return minBatteryPackVoltage;
         }

      @Override
      public Double getMinBatteryPackCurrent()
         {
         return minBatteryPackCurrent;
         }

      @Override
      public Double getMaxBatteryPackCurrent()
         {
         return maxBatteryPackCurrent;
         }

      @Override
      public Double getMaxBatteryPackVoltage()
         {
         return maxBatteryPackVoltage;
         }
      }

   private static final class EnergyEquationImpl implements EnergyEquation
      {
      private static final String TO_STRING_DELIMITER = "\t";

      private double kwhDelta = 0.0;
      private double kwhUsed = 0.0;
      private double kwhRegen = 0.0;

      public double getKilowattHours()
         {
         return kwhUsed + kwhRegen;
         }

      public double getKilowattHoursDelta()
         {
         return kwhDelta;
         }

      public double getKilowattHoursUsed()
         {
         return kwhUsed;
         }

      public double getKilowattHoursRegen()
         {
         return kwhRegen;
         }

      private void addKilowattHours(final double kwh)
         {
         kwhDelta = kwh;

         if (Double.compare(kwh, 0.0) > 0)
            {
            kwhUsed += kwh;
            }
         else if (Double.compare(kwh, 0.0) < 0)
            {
            kwhRegen += kwh;
            }
         }

      public void reset()
         {
         LOG.info("BMSModel$EnergyEquationImpl.reset(): Resetting battery energy equation");
         kwhDelta = 0.0;
         kwhUsed = 0.0;
         kwhRegen = 0.0;
         }

      @Override
      public String toString()
         {
         final StringBuilder sb = new StringBuilder();
         sb.append("EnergyEquation");
         sb.append("{kwhDelta=").append(kwhDelta);
         sb.append(", kwhUsed=").append(kwhUsed);
         sb.append(", kwhRegen=").append(kwhRegen);
         sb.append('}');
         return sb.toString();
         }

      private String toLoggingString(final long timestampMilliseconds)
         {
         final StringBuilder sb = new StringBuilder();
         sb.append("EnergyEquation");
         sb.append(TO_STRING_DELIMITER);
         sb.append(timestampMilliseconds).append(TO_STRING_DELIMITER);
         sb.append(getKilowattHours()).append(TO_STRING_DELIMITER);
         sb.append(kwhUsed).append(TO_STRING_DELIMITER);
         sb.append(kwhRegen).append(TO_STRING_DELIMITER);
         sb.append(kwhDelta);
         return sb.toString();
         }
      }
   }
