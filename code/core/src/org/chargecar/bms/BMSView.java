package org.chargecar.bms;

import java.awt.Color;
import java.awt.Font;
import java.text.DecimalFormat;
import java.util.PropertyResourceBundle;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;
import edu.cmu.ri.createlab.userinterface.GUIConstants;
import edu.cmu.ri.createlab.userinterface.util.SwingUtils;
import org.apache.log4j.Logger;
import org.chargecar.serial.streaming.StreamingSerialPortDeviceView;
import org.chargecar.userinterface.ChartMouseAdapter;
import org.chargecar.userinterface.DefaultMeterConfig;
import org.chargecar.userinterface.Gauge;
import org.chargecar.userinterface.Meter;
import org.chargecar.userinterface.UserInterfaceConstants;
import org.jfree.chart.ChartMouseEvent;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
public final class BMSView extends StreamingSerialPortDeviceView<BMSAndEnergy>
   {
   private static final Logger LOG = Logger.getLogger(BMSView.class);
   private static final PropertyResourceBundle RESOURCES = (PropertyResourceBundle)PropertyResourceBundle.getBundle(BMSView.class.getName());

   private final FaultStatusPanel faultStatusPanel = new FaultStatusPanel();

   private final Meter batteryCurrentMeter;
   private final Meter batteryVoltageMeter;

   private final Gauge<Double> packTotalVoltageGauge = new Gauge<Double>(RESOURCES.getString("label.pack-total-voltage"), "%6.2f");
   private final Gauge<Double> minimumCellVoltageGauge = new Gauge<Double>(RESOURCES.getString("label.minimum-cell-voltage"), "%4.2f");
   private final Gauge<Double> maximumCellVoltageGauge = new Gauge<Double>(RESOURCES.getString("label.maximum-cell-voltage"), "%4.2f");
   private final Gauge<Double> averageCellVoltageGauge = new Gauge<Double>(RESOURCES.getString("label.average-cell-voltage"), "%4.2f");
   private final Gauge<Integer> cellNumWithLowestVoltageGauge = new Gauge<Integer>(RESOURCES.getString("label.cell-num-with-lowest-voltage"), "%2d");
   private final Gauge<Integer> cellNumWithHighestVoltageGauge = new Gauge<Integer>(RESOURCES.getString("label.cell-num-with-highest-voltage"), "%2d");

   private final Gauge<Integer> minimumCellTempGauge = new Gauge<Integer>(RESOURCES.getString("label.minimum-cell-temp"), "%2d");
   private final Gauge<Integer> maximumCellTempGauge = new Gauge<Integer>(RESOURCES.getString("label.maximum-cell-temp"), "%2d");
   private final Gauge<Integer> averageCellTempGauge = new Gauge<Integer>(RESOURCES.getString("label.average-cell-temp"), "%2d");
   private final Gauge<Integer> cellNumWithLowestTempGauge = new Gauge<Integer>(RESOURCES.getString("label.cell-num-with-lowest-temp"), "%2d");
   private final Gauge<Integer> cellNumWithHighestTempGauge = new Gauge<Integer>(RESOURCES.getString("label.cell-num-with-highest-temp"), "%2d");

   private final Gauge<Boolean> isLLIMSetGauge = new Gauge<Boolean>(RESOURCES.getString("label.is-llim-set"), "%s");
   private final Gauge<Boolean> isHLIMSetGauge = new Gauge<Boolean>(RESOURCES.getString("label.is-hlim-set"), "%s");

   private final Gauge<Double> loadCurrentAmpsGauge = new Gauge<Double>(RESOURCES.getString("label.load-current-amps"), "%6.2f");
   private final Gauge<Integer> depthOfDischargeGauge = new Gauge<Integer>(RESOURCES.getString("label.depth-of-discharge"), "%d");

   private final Gauge<Integer> stateOfChargeGauge = new Gauge<Integer>(RESOURCES.getString("label.state-of-charge"), "%d");
   private final Gauge<Integer> stateOfHealthGauge = new Gauge<Integer>(RESOURCES.getString("label.state-of-health"), "%d");

   private final Gauge<Boolean> isInterlockTrippedGauge = new Gauge<Boolean>(RESOURCES.getString("label.is-interlock-tripped"), "%s");
   private final Gauge<Boolean> isCommunicationFaultWithBankOrCellGauge = new Gauge<Boolean>(RESOURCES.getString("label.is-communication-fault-with-bank-or-cell"), "%s");
   private final Gauge<Boolean> isChargeOvercurrentGauge = new Gauge<Boolean>(RESOURCES.getString("label.is-charge-overcurrent"), "%s");
   private final Gauge<Boolean> isDischargeOvercurrentGauge = new Gauge<Boolean>(RESOURCES.getString("label.is-discharge-overcurrent"), "%s");
   private final Gauge<Boolean> isOverTemperatureGauge = new Gauge<Boolean>(RESOURCES.getString("label.is-over-temperature"), "%s");
   private final Gauge<Boolean> isUnderVoltageGauge = new Gauge<Boolean>(RESOURCES.getString("label.is-under-voltage"), "%s");
   private final Gauge<Boolean> isOverVoltageGauge = new Gauge<Boolean>(RESOURCES.getString("label.is-over-voltage"), "%s");

   private final Gauge<Double> batteryEnergyTotalGauge = new Gauge<Double>(RESOURCES.getString("label.total"), "%07.3f");
   private final Gauge<Double> batteryEnergyUsedGauge = new Gauge<Double>(RESOURCES.getString("label.used"), "%07.3f");
   private final Gauge<Double> batteryEnergyRegenGauge = new Gauge<Double>(RESOURCES.getString("label.regen"), "%07.3f");

   private final BatteriesTableModel batteriesTableModel;

   public BMSView(final BMSController bmsController, final BMSModel bmsModel)
      {
      batteriesTableModel = new BatteriesTableModel(bmsModel.getNumBatteries());

      final DefaultMeterConfig meterConfig = new DefaultMeterConfig(3);
      meterConfig.setDatasetColor(0, Color.RED);
      meterConfig.setDatasetColor(1, Color.GREEN);
      meterConfig.setDatasetColor(2, Color.BLUE);

      // configure the meters ==========================================================================================

      meterConfig.setSize(350, 350);
      meterConfig.setRange(0, 1000);
      meterConfig.setTicks(100, 9);
      meterConfig.setNumberFormat(new DecimalFormat("###0.0"));
      meterConfig.setLabel(RESOURCES.getString("label.battery-current"));

      batteryCurrentMeter = new Meter(meterConfig);

      // ---------------------------------------------------------------------------------------------------------------

      meterConfig.clearDialRanges();
      meterConfig.setRange(120, 170);
      meterConfig.setTicks(5, 4);
      meterConfig.addDialRange(120, 130, Color.RED);
      meterConfig.addDialRange(130, 135, new Color(255, 150, 0));
      meterConfig.setNumberFormat(new DecimalFormat("##0.0"));
      meterConfig.setLabel(RESOURCES.getString("label.battery-voltage"));

      batteryVoltageMeter = new Meter(meterConfig);

      // ---------------------------------------------------------------------------------------------------------------

      // Add mouse listeners for all the meters
      batteryCurrentMeter.addChartMouseListener(
            new MeterResetHandler()
            {
            public void resetMeter()
               {
               LOG.debug("BMSView.resetMeter(): reset battery pack min/max currents...");
               bmsController.resetBatteryPackCurrentMinMax();
               }
            }
      );
      batteryVoltageMeter.addChartMouseListener(
            new MeterResetHandler()
            {
            public void resetMeter()
               {
               LOG.debug("BMSView.resetMeter(): reset battery pack min/max voltages...");
               bmsController.resetBatteryPackVoltageMinMax();
               }
            }
      );
      }

   protected void handleEventInGUIThread(final BMSAndEnergy bmsAndEnergy)
      {
      if (bmsAndEnergy != null && bmsAndEnergy.getBmsState() != null)
         {
         final BMSEvent eventData = bmsAndEnergy.getBmsState();
         faultStatusPanel.setValue(eventData.getBMSFault());

         batteryVoltageMeter.setValues(eventData.getPackTotalVoltage(),
                                       bmsAndEnergy.getMinBatteryPackVoltage(),
                                       bmsAndEnergy.getMaxBatteryPackVoltage());
         batteryCurrentMeter.setValues(eventData.getLoadCurrentAmps(),
                                       bmsAndEnergy.getMinBatteryPackCurrent(),
                                       bmsAndEnergy.getMaxBatteryPackCurrent());

         packTotalVoltageGauge.setValue(eventData.getPackTotalVoltage());
         minimumCellVoltageGauge.setValue(eventData.getMinimumCellVoltage());
         maximumCellVoltageGauge.setValue(eventData.getMaximumCellVoltage());
         averageCellVoltageGauge.setValue(eventData.getAverageCellVoltage());
         cellNumWithLowestVoltageGauge.setValue(eventData.getCellNumWithLowestVoltage());
         cellNumWithHighestVoltageGauge.setValue(eventData.getCellNumWithHighestVoltage());

         minimumCellTempGauge.setValue(eventData.getMinimumCellBoardTemp());
         maximumCellTempGauge.setValue(eventData.getMaximumCellBoardTemp());
         averageCellTempGauge.setValue(eventData.getAverageCellBoardTemp());
         cellNumWithLowestTempGauge.setValue(eventData.getCellBoardNumWithLowestTemp());
         cellNumWithHighestTempGauge.setValue(eventData.getCellBoardNumWithHighestTemp());

         isLLIMSetGauge.setValue(eventData.isLLIMSet(), eventData.isLLIMSet() ? UserInterfaceConstants.RED : UserInterfaceConstants.GREEN);
         isHLIMSetGauge.setValue(eventData.isHLIMSet(), eventData.isHLIMSet() ? UserInterfaceConstants.RED : UserInterfaceConstants.GREEN);

         loadCurrentAmpsGauge.setValue(eventData.getLoadCurrentAmps());
         depthOfDischargeGauge.setValue(eventData.getDepthOfDischarge());

         stateOfChargeGauge.setValue(eventData.getStateOfChargePercentage());
         stateOfHealthGauge.setValue(eventData.getStateOfHealthPercentage());

         isInterlockTrippedGauge.setValue(eventData.isInterlockTripped2(), eventData.isInterlockTripped2() ? UserInterfaceConstants.RED : UserInterfaceConstants.GREEN);
         isCommunicationFaultWithBankOrCellGauge.setValue(eventData.isCommunicationFaultWithBankOrCell(), eventData.isCommunicationFaultWithBankOrCell() ? UserInterfaceConstants.RED : UserInterfaceConstants.GREEN);
         isChargeOvercurrentGauge.setValue(eventData.isChargeOvercurrent(), eventData.isChargeOvercurrent() ? UserInterfaceConstants.RED : UserInterfaceConstants.GREEN);
         isDischargeOvercurrentGauge.setValue(eventData.isDischargeOvercurrent(), eventData.isDischargeOvercurrent() ? UserInterfaceConstants.RED : UserInterfaceConstants.GREEN);
         isOverTemperatureGauge.setValue(eventData.isOverTemperature(), eventData.isOverTemperature() ? UserInterfaceConstants.RED : UserInterfaceConstants.GREEN);
         isUnderVoltageGauge.setValue(eventData.isUnderVoltage(), eventData.isUnderVoltage() ? UserInterfaceConstants.RED : UserInterfaceConstants.GREEN);
         isOverVoltageGauge.setValue(eventData.isOverVoltage(), eventData.isOverVoltage() ? UserInterfaceConstants.RED : UserInterfaceConstants.GREEN);

         batteryEnergyTotalGauge.setValue(bmsAndEnergy.getEnergyEquation().getKilowattHours());
         batteryEnergyUsedGauge.setValue(bmsAndEnergy.getEnergyEquation().getKilowattHoursUsed());
         batteryEnergyRegenGauge.setValue(bmsAndEnergy.getEnergyEquation().getKilowattHoursRegen());

         batteriesTableModel.setVoltages(bmsAndEnergy.getBmsState().getCellVoltages());
         batteriesTableModel.setTemperatures(bmsAndEnergy.getBmsState().getCellTemperatures());
         }
      else
         {
         batteryVoltageMeter.setValues(null, null, null);
         batteryCurrentMeter.setValues(null, null, null);

         faultStatusPanel.setValue(null);

         packTotalVoltageGauge.setValue(null);
         minimumCellVoltageGauge.setValue(null);
         maximumCellVoltageGauge.setValue(null);
         averageCellVoltageGauge.setValue(null);
         cellNumWithLowestVoltageGauge.setValue(null);
         cellNumWithHighestVoltageGauge.setValue(null);

         minimumCellTempGauge.setValue(null);
         maximumCellTempGauge.setValue(null);
         averageCellTempGauge.setValue(null);
         cellNumWithLowestTempGauge.setValue(null);
         cellNumWithHighestTempGauge.setValue(null);

         isLLIMSetGauge.setValue(null);
         isHLIMSetGauge.setValue(null);

         loadCurrentAmpsGauge.setValue(null);
         depthOfDischargeGauge.setValue(null);

         stateOfChargeGauge.setValue(null);
         stateOfHealthGauge.setValue(null);

         isInterlockTrippedGauge.setValue(null);
         isCommunicationFaultWithBankOrCellGauge.setValue(null);
         isChargeOvercurrentGauge.setValue(null);
         isDischargeOvercurrentGauge.setValue(null);
         isOverTemperatureGauge.setValue(null);
         isUnderVoltageGauge.setValue(null);
         isOverVoltageGauge.setValue(null);

         batteryEnergyTotalGauge.setValue(null);
         batteryEnergyUsedGauge.setValue(null);
         batteryEnergyRegenGauge.setValue(null);

         batteriesTableModel.setVoltages(null);
         batteriesTableModel.setTemperatures(null);
         }
      }

   public Meter getBatteryCurrentMeter()
      {
      return batteryCurrentMeter;
      }

   public Meter getBatteryVoltageMeter()
      {
      return batteryVoltageMeter;
      }

   public JPanel getFaultStatusPanel()
      {
      return faultStatusPanel;
      }

   public Gauge<Double> getPackTotalVoltageGauge()
      {
      return packTotalVoltageGauge;
      }

   public Gauge<Double> getMinimumCellVoltageGauge()
      {
      return minimumCellVoltageGauge;
      }

   public Gauge<Double> getMaximumCellVoltageGauge()
      {
      return maximumCellVoltageGauge;
      }

   public Gauge<Double> getAverageCellVoltageGauge()
      {
      return averageCellVoltageGauge;
      }

   public Gauge<Integer> getCellNumWithLowestVoltageGauge()
      {
      return cellNumWithLowestVoltageGauge;
      }

   public Gauge<Integer> getCellNumWithHighestVoltageGauge()
      {
      return cellNumWithHighestVoltageGauge;
      }

   public Gauge<Integer> getMinimumCellTempGauge()
      {
      return minimumCellTempGauge;
      }

   public Gauge<Integer> getMaximumCellTempGauge()
      {
      return maximumCellTempGauge;
      }

   public Gauge<Integer> getAverageCellTempGauge()
      {
      return averageCellTempGauge;
      }

   public Gauge<Integer> getCellNumWithLowestTempGauge()
      {
      return cellNumWithLowestTempGauge;
      }

   public Gauge<Integer> getCellNumWithHighestTempGauge()
      {
      return cellNumWithHighestTempGauge;
      }

   public Gauge<Boolean> getLLIMSetGauge()
      {
      return isLLIMSetGauge;
      }

   public Gauge<Boolean> getHLIMSetGauge()
      {
      return isHLIMSetGauge;
      }

   public Gauge<Double> getLoadCurrentAmpsGauge()
      {
      return loadCurrentAmpsGauge;
      }

   public Gauge<Integer> getDepthOfDischargeGauge()
      {
      return depthOfDischargeGauge;
      }

   public Gauge<Integer> getStateOfChargeGauge()
      {
      return stateOfChargeGauge;
      }

   public Gauge<Integer> getStateOfHealthGauge()
      {
      return stateOfHealthGauge;
      }

   public Gauge<Boolean> getInterlockTrippedGauge()
      {
      return isInterlockTrippedGauge;
      }

   public Gauge<Boolean> getCommunicationFaultWithBankOrCellGauge()
      {
      return isCommunicationFaultWithBankOrCellGauge;
      }

   public Gauge<Boolean> getChargeOvercurrentGauge()
      {
      return isChargeOvercurrentGauge;
      }

   public Gauge<Boolean> getDischargeOvercurrentGauge()
      {
      return isDischargeOvercurrentGauge;
      }

   public Gauge<Boolean> getOverTemperatureGauge()
      {
      return isOverTemperatureGauge;
      }

   public Gauge<Boolean> getUnderVoltageGauge()
      {
      return isUnderVoltageGauge;
      }

   public Gauge<Boolean> getOverVoltageGauge()
      {
      return isOverVoltageGauge;
      }

   public Gauge getBatteryEnergyTotalGauge()
      {
      return batteryEnergyTotalGauge;
      }

   public Gauge getBatteryEnergyUsedGauge()
      {
      return batteryEnergyUsedGauge;
      }

   public Gauge getBatteryEnergyRegenGauge()
      {
      return batteryEnergyRegenGauge;
      }

   public BatteriesTableModel getBatteriesTableModel()
      {
      return batteriesTableModel;
      }

   private final class FaultStatusPanel extends JPanel
      {
      private final JLabel value = SwingUtils.createLabel(UserInterfaceConstants.UNKNOWN_VALUE,
                                                          new Font(GUIConstants.FONT_MEDIUM.getFontName(),
                                                                   Font.BOLD,
                                                                   GUIConstants.FONT_MEDIUM.getSize()));

      private FaultStatusPanel()
         {
         this.setBackground(Color.WHITE);
         this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
         this.add(Box.createGlue());
         this.add(SwingUtils.createLabel(RESOURCES.getString("label.bms-status") + ":",
                                         new Font(GUIConstants.FONT_MEDIUM.getFontName(),
                                                  Font.PLAIN,
                                                  GUIConstants.FONT_MEDIUM.getSize())));
         this.add(SwingUtils.createRigidSpacer());
         this.add(value);
         this.add(Box.createGlue());
         }

      private void setValue(final BMSFault bmsFault)
         {
         if (bmsFault != null)
            {
            if (BMSFault.CODE_0.equals(bmsFault))
               {
               value.setForeground(UserInterfaceConstants.GREEN);
               value.setText(bmsFault.getMessage());
               }
            else
               {
               value.setForeground(UserInterfaceConstants.RED);
               value.setText(bmsFault.getMessageAndCode());
               }
            }
         else
            {
            value.setForeground(UserInterfaceConstants.RED);
            value.setText(UserInterfaceConstants.UNKNOWN_VALUE);
            }
         }
      }

   private final class BatteriesTableModel extends AbstractTableModel
      {
      private final int numBatteries;

      private final double[] defaultVoltages;
      private final int[] defaultTemperatures;

      private final double[] voltages;
      private final int[] temperatures;

      private BatteriesTableModel(final int numBatteries)
         {
         this.numBatteries = numBatteries;

         defaultVoltages = new double[numBatteries];  // default value is zero
         defaultTemperatures = new int[numBatteries]; // default value is zero

         voltages = new double[numBatteries];
         temperatures = new int[numBatteries];
         }

      @Override
      public String getColumnName(final int col)
         {
         switch (col)
            {
            case 0:
               return RESOURCES.getString("label.battery-table.battery");
            case 1:
               return RESOURCES.getString("label.battery-table.voltage");
            case 2:
               return RESOURCES.getString("label.battery-table.temperature");
            default:
               LOG.error("Unexpected column indeax [" + col + "]");
            }
         return "???";
         }

      @Override
      public Class<?> getColumnClass(final int col)
         {
         switch (col)
            {
            case 0:
               return Integer.class;
            case 1:
               return Double.class;
            case 2:
               return Integer.class;
            default:
               LOG.error("Unexpected column indeax [" + col + "]");
            }
         return Object.class;
         }

      @Override
      public int getRowCount()
         {
         return numBatteries;
         }

      @Override
      public int getColumnCount()
         {
         return 3;
         }

      @Override
      public Object getValueAt(final int row, final int col)
         {
         switch (col)
            {
            case 0:
               return row + 1;
            case 1:
               return voltages[row];
            case 2:
               return temperatures[row];
            default:
               LOG.error("Unexpected column indeax [" + col + "]");
            }
         return null;
         }

      private void setVoltages(final double[] newVoltages)
         {
         final double[] newData;
         if (newVoltages != null && newVoltages.length >= numBatteries)
            {
            newData = newVoltages;
            }
         else
            {
            newData = defaultVoltages;
            }
         // copy in the new data
         System.arraycopy(newData, 0, voltages, 0, numBatteries);

         // fire the update event
         fireTableRowsUpdated(0, numBatteries - 1);
         }

      private void setTemperatures(final int[] newTemperatures)
         {
         final int[] newData;
         if (newTemperatures != null && newTemperatures.length >= numBatteries)
            {
            newData = newTemperatures;
            }
         else
            {
            newData = defaultTemperatures;
            }
         // copy in the new data
         System.arraycopy(newData, 0, temperatures, 0, numBatteries);

         // fire the update event
         fireTableRowsUpdated(0, numBatteries - 1);
         }
      }

   private abstract static class MeterResetHandler extends ChartMouseAdapter
      {
      public final void chartMouseClicked(final ChartMouseEvent event)
         {
         final SwingWorker sw =
               new SwingWorker<Object, Object>()
               {
               @Override
               protected Object doInBackground() throws Exception
                  {
                  resetMeter();
                  return null;
                  }
               };
         sw.execute();
         }

      protected abstract void resetMeter();
      }
   }
