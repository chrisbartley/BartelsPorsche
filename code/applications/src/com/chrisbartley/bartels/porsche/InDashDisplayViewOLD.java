package com.chrisbartley.bartels.porsche;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.PropertyResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import edu.cmu.ri.createlab.userinterface.GUIConstants;
import edu.cmu.ri.createlab.userinterface.util.AbstractTimeConsumingAction;
import edu.cmu.ri.createlab.userinterface.util.SwingUtils;
import org.apache.log4j.Logger;
import org.chargecar.bms.BMSController;
import org.chargecar.bms.BMSModel;
import org.chargecar.bms.BMSView;
import org.chargecar.serial.streaming.StreamingSerialPortDeviceConnectionStateListener;
import org.chargecar.userinterface.UserInterfaceConstants;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
@SuppressWarnings({"CloneableClassWithoutClone"})
public final class InDashDisplayViewOLD extends JPanel
   {
   private static final Logger LOG = Logger.getLogger(InDashDisplayViewOLD.class);
   private static final Logger DATA_LOG = Logger.getLogger("DataLog");

   private static final PropertyResourceBundle RESOURCES = (PropertyResourceBundle)PropertyResourceBundle.getBundle(InDashDisplayViewOLD.class.getName());

   public InDashDisplayViewOLD(final InDashDisplayController inDashDisplayController,
                               final BMSController bmsController,
                               final BMSModel bmsModel,
                               final BMSView bmsView)
      {

      final AtomicInteger markValue = new AtomicInteger(0);
      final JButton quitButton = SwingUtils.createButton(RESOURCES.getString("label.quit"), true);
      final JButton markButton = SwingUtils.createButton(RESOURCES.getString("label.mark") + " " + markValue.get(), true);
      final JButton resetBatteryEnergyButton = SwingUtils.createButton(RESOURCES.getString("label.reset"), true);

      this.setBackground(Color.WHITE);

      quitButton.addActionListener(
            new ButtonTimeConsumingAction(this, quitButton)
            {
            protected Object executeTimeConsumingAction()
               {
               inDashDisplayController.shutdown();
               return null;
               }
            });

      final ActionListener markButtonActionListener =
            new ActionListener()
            {
            public void actionPerformed(final ActionEvent e)
               {
               final int value = markValue.getAndIncrement();
               final String message = "============================================================= MARK " + value + " (" + System.currentTimeMillis() + ") =============================================================";
               LOG.info(message);
               DATA_LOG.info(message);
               markButton.setText(RESOURCES.getString("label.mark") + " " + value);
               }
            };

      markButton.addActionListener(markButtonActionListener);

      resetBatteryEnergyButton.addActionListener(
            new ButtonTimeConsumingAction(this, resetBatteryEnergyButton)
            {
            protected Object executeTimeConsumingAction()
               {
               bmsController.resetBatteryEnergyEquation();
               markButtonActionListener.actionPerformed(null);  // log a mark here as well
               return null;
               }
            });

      // create the connection state status labels
      final JLabel bmsConnectionState = SwingUtils.createLabel(RESOURCES.getString("label.bms"));
      final JLabel gpsConnectionState = SwingUtils.createLabel(RESOURCES.getString("label.gps"));
      final JLabel motorControllerConnectionState = SwingUtils.createLabel(RESOURCES.getString("label.motor-controller"));
      final JLabel sensorBoardConnectionState = SwingUtils.createLabel(RESOURCES.getString("label.sensor-board"));

      // set the initial color to red
      bmsConnectionState.setForeground(UserInterfaceConstants.RED);
      gpsConnectionState.setForeground(UserInterfaceConstants.RED);
      motorControllerConnectionState.setForeground(UserInterfaceConstants.RED);
      sensorBoardConnectionState.setForeground(UserInterfaceConstants.RED);

      // configure the connection state status labels
      bmsConnectionState.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.BLACK),
                                                                      BorderFactory.createEmptyBorder(3, 3, 3, 3)));
      gpsConnectionState.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.BLACK),
                                                                      BorderFactory.createEmptyBorder(3, 3, 3, 3)));
      motorControllerConnectionState.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.BLACK),
                                                                                  BorderFactory.createEmptyBorder(3, 3, 3, 3)));
      sensorBoardConnectionState.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.BLACK),
                                                                              BorderFactory.createEmptyBorder(3, 3, 3, 3)));

      final JLabel batteryEquationEquals = SwingUtils.createLabel(RESOURCES.getString("label.equals"), GUIConstants.FONT_MEDIUM_LARGE);
      final JLabel batteryEquationPlus = SwingUtils.createLabel(RESOURCES.getString("label.plus"), GUIConstants.FONT_MEDIUM_LARGE);

      final JPanel row1 = new JPanel();
      row1.setBackground(Color.WHITE);
      row1.setLayout(new BoxLayout(row1, BoxLayout.X_AXIS));
      row1.add(Box.createGlue());
      row1.add(quitButton);
      row1.add(Box.createGlue());
      row1.add(bmsConnectionState);
      row1.add(SwingUtils.createRigidSpacer(10));
      row1.add(gpsConnectionState);
      row1.add(SwingUtils.createRigidSpacer(10));
      row1.add(motorControllerConnectionState);
      row1.add(SwingUtils.createRigidSpacer(10));
      row1.add(sensorBoardConnectionState);
      row1.add(Box.createGlue());
      row1.add(markButton);
      row1.add(Box.createGlue());

      final JPanel row2 = new JPanel();
      row2.setBackground(Color.WHITE);
      row2.setLayout(new BoxLayout(row2, BoxLayout.X_AXIS));
      row2.add(Box.createGlue());
      row2.add(bmsView.getFaultStatusPanel());
      row2.add(Box.createGlue());

      final Component horizontalSpacer1 = SwingUtils.createRigidSpacer(30);
      final Component horizontalSpacer2 = SwingUtils.createRigidSpacer(30);
      final Component horizontalSpacer3 = SwingUtils.createRigidSpacer(30);
      final Component horizontalSpacer4 = SwingUtils.createRigidSpacer(30);
      final Component horizontalSpacer5 = SwingUtils.createRigidSpacer(30);
      final Component deadSpace = SwingUtils.createRigidSpacer(30);

      final Component verticalSpacer1 = SwingUtils.createRigidSpacer(20);
      final Component verticalSpacer2 = SwingUtils.createRigidSpacer(20);
      final Component verticalSpacer3 = SwingUtils.createRigidSpacer(20);
      final Component verticalSpacer4 = SwingUtils.createRigidSpacer(20);
      final Component verticalSpacer5 = SwingUtils.createRigidSpacer(20);
      final Component verticalSpacer6 = SwingUtils.createRigidSpacer(20);

      final JLabel notAvailable1 = new JLabel("n/a 1");
      final JLabel notAvailable2 = new JLabel("n/a 2");
      final JLabel notAvailable3 = new JLabel("n/a 3");

      final JPanel grid = new JPanel();
      final GroupLayout layout = new GroupLayout(grid);
      grid.setBackground(Color.WHITE);
      grid.setLayout(layout);
      layout.setHorizontalGroup(
            layout.createSequentialGroup()
                  .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                  .addComponent(deadSpace)
                                  .addComponent(horizontalSpacer5)
                                  .addComponent(bmsView.getOverTemperatureGauge())
                  )
                  .addComponent(verticalSpacer1)
                  .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                  .addComponent(notAvailable1)
                                  .addComponent(horizontalSpacer1)
                                  .addComponent(bmsView.getMinimumCellTempGauge())
                                  .addComponent(horizontalSpacer2)
                                  .addComponent(bmsView.getMinimumCellVoltageGauge())
                                  .addComponent(horizontalSpacer3)
                                  .addComponent(bmsView.getPackTotalVoltageGauge())
                                  .addComponent(horizontalSpacer4)
                                  .addComponent(bmsView.getBatteryEnergyTotalGauge())
                                  .addComponent(bmsView.getUnderVoltageGauge())
                  )
                  .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                  .addComponent(verticalSpacer2)
                  )
                  .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                  .addComponent(notAvailable2)
                                  .addComponent(bmsView.getMaximumCellTempGauge())
                                  .addComponent(bmsView.getMaximumCellVoltageGauge())
                                  .addComponent(bmsView.getLoadCurrentAmpsGauge())
                                  .addComponent(batteryEquationEquals)
                                  .addComponent(bmsView.getOverVoltageGauge())
                  )
                  .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                  .addComponent(verticalSpacer3)
                  )
                  .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                  .addComponent(notAvailable3)
                                  .addComponent(bmsView.getAverageCellTempGauge())
                                  .addComponent(bmsView.getAverageCellVoltageGauge())
                                  .addComponent(bmsView.getDepthOfDischargeGauge())
                                  .addComponent(bmsView.getBatteryEnergyUsedGauge())
                                  .addComponent(bmsView.getChargeOvercurrentGauge())
                  )
                  .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                  .addComponent(verticalSpacer4)
                  )
                  .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                  .addComponent(bmsView.getLLIMSetGauge())
                                  .addComponent(bmsView.getCellNumWithLowestTempGauge())
                                  .addComponent(bmsView.getCellNumWithLowestVoltageGauge())
                                  .addComponent(bmsView.getStateOfChargeGauge())
                                  .addComponent(batteryEquationPlus)
                                  .addComponent(bmsView.getDischargeOvercurrentGauge())
                  )
                  .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                  .addComponent(verticalSpacer5)
                  )
                  .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                  .addComponent(bmsView.getHLIMSetGauge())
                                  .addComponent(bmsView.getCellNumWithHighestTempGauge())
                                  .addComponent(bmsView.getCellNumWithHighestVoltageGauge())
                                  .addComponent(bmsView.getStateOfHealthGauge())
                                  .addComponent(bmsView.getBatteryEnergyRegenGauge())
                                  .addComponent(bmsView.getCommunicationFaultWithBankOrCellGauge())
                  )
                  .addComponent(verticalSpacer6)
                  .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                  .addComponent(resetBatteryEnergyButton)
                                  .addComponent(bmsView.getInterlockTrippedGauge())
                  )
      );
      layout.setVerticalGroup(
            layout.createSequentialGroup()
                  .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                  .addComponent(verticalSpacer1)
                                  .addComponent(notAvailable1)
                                  .addComponent(verticalSpacer2)
                                  .addComponent(notAvailable2)
                                  .addComponent(verticalSpacer3)
                                  .addComponent(notAvailable3)
                                  .addComponent(verticalSpacer4)
                                  .addComponent(bmsView.getLLIMSetGauge())
                                  .addComponent(verticalSpacer5)
                                  .addComponent(bmsView.getHLIMSetGauge())
                                  .addComponent(verticalSpacer6)
                  )
                  .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                  .addComponent(horizontalSpacer1)
                  )
                  .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                  .addComponent(bmsView.getMinimumCellTempGauge())
                                  .addComponent(bmsView.getMaximumCellTempGauge())
                                  .addComponent(bmsView.getAverageCellTempGauge())
                                  .addComponent(bmsView.getCellNumWithLowestTempGauge())
                                  .addComponent(bmsView.getCellNumWithHighestTempGauge())
                  )
                  .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                  .addComponent(horizontalSpacer2)
                  )
                  .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                  .addComponent(bmsView.getMinimumCellVoltageGauge())
                                  .addComponent(bmsView.getMaximumCellVoltageGauge())
                                  .addComponent(bmsView.getAverageCellVoltageGauge())
                                  .addComponent(bmsView.getCellNumWithLowestVoltageGauge())
                                  .addComponent(bmsView.getCellNumWithHighestVoltageGauge())
                  )
                  .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                  .addComponent(horizontalSpacer3)
                  )
                  .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                  .addComponent(bmsView.getPackTotalVoltageGauge())
                                  .addComponent(bmsView.getLoadCurrentAmpsGauge())
                                  .addComponent(bmsView.getDepthOfDischargeGauge())
                                  .addComponent(bmsView.getStateOfChargeGauge())
                                  .addComponent(bmsView.getStateOfHealthGauge())
                  )
                  .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                  .addComponent(horizontalSpacer4)
                  )
                  .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                  .addComponent(deadSpace)
                                  .addComponent(bmsView.getBatteryEnergyTotalGauge())
                                  .addComponent(batteryEquationEquals)
                                  .addComponent(bmsView.getBatteryEnergyUsedGauge())
                                  .addComponent(batteryEquationPlus)
                                  .addComponent(bmsView.getBatteryEnergyRegenGauge())
                                  .addComponent(resetBatteryEnergyButton)
                  )
                  .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                  .addComponent(horizontalSpacer5)
                  )
                  .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                  .addComponent(bmsView.getOverTemperatureGauge())
                                  .addComponent(bmsView.getUnderVoltageGauge())
                                  .addComponent(bmsView.getOverVoltageGauge())
                                  .addComponent(bmsView.getChargeOvercurrentGauge())
                                  .addComponent(bmsView.getDischargeOvercurrentGauge())
                                  .addComponent(bmsView.getCommunicationFaultWithBankOrCellGauge())
                                  .addComponent(bmsView.getInterlockTrippedGauge())
                  )
      );

      // register self as a connection state listener for the various models so we can display connection status
      bmsModel.addStreamingSerialPortDeviceConnectionStateListener(new DeviceConnectionStateListener(bmsConnectionState));

      this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
      this.add(Box.createGlue());
      this.add(row1);
      this.add(SwingUtils.createRigidSpacer(20));
      this.add(row2);
      this.add(SwingUtils.createRigidSpacer(20));
      this.add(grid);
      this.add(Box.createGlue());
      }

   private abstract static class ButtonTimeConsumingAction extends AbstractTimeConsumingAction
      {
      private final JButton button;

      private ButtonTimeConsumingAction(final Component parentComponent, final JButton button)
         {
         super(parentComponent);
         this.button = button;
         }

      @Override
      protected final void executeGUIActionBefore()
         {
         super.executeGUIActionBefore();
         button.setEnabled(false);
         }

      @Override
      protected void executeGUIActionAfter(final Object resultOfTimeConsumingAction)
         {
         super.executeGUIActionAfter(resultOfTimeConsumingAction);
         button.setEnabled(true);
         }
      }

   private final class DeviceConnectionStateListener implements StreamingSerialPortDeviceConnectionStateListener
      {
      private final JLabel label;
      private final Runnable isConnectedRunnable =
            new Runnable()
            {
            public void run()
               {
               label.setForeground(UserInterfaceConstants.GREEN);
               }
            };
      private final Runnable isDisconnectedRunnable =
            new Runnable()
            {
            public void run()
               {
               label.setForeground(UserInterfaceConstants.RED);
               }
            };

      private DeviceConnectionStateListener(final JLabel label)
         {
         this.label = label;
         }

      public void handleConnectionStateChange(final boolean isConnected)
         {
         SwingUtils.runInGUIThread(isConnected ? isConnectedRunnable : isDisconnectedRunnable);
         }
      }
   }
