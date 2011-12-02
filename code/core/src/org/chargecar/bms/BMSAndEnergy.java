package org.chargecar.bms;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
public interface BMSAndEnergy
   {
   BMSEvent getBmsState();

   EnergyEquation getEnergyEquation();

   Double getMinBatteryPackVoltage();

   Double getMinBatteryPackCurrent();

   Double getMaxBatteryPackCurrent();

   Double getMaxBatteryPackVoltage();
   }