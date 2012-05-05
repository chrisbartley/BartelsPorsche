package com.chrisbartley.bartels.porsche;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.PropertyResourceBundle;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.border.Border;
import edu.cmu.ri.createlab.userinterface.util.AbstractTimeConsumingAction;
import edu.cmu.ri.createlab.userinterface.util.SwingUtils;
import org.chargecar.bms.BMSModel;
import org.chargecar.bms.BMSView;
import org.chargecar.serial.streaming.StreamingSerialPortDeviceConnectionStateListener;
import org.chargecar.userinterface.Meter;
import org.chargecar.userinterface.UserInterfaceConstants;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
@SuppressWarnings({"CloneableClassWithoutClone"})
public final class InDashDisplayView extends JPanel
   {
   private static final PropertyResourceBundle RESOURCES = (PropertyResourceBundle)PropertyResourceBundle.getBundle(InDashDisplayView.class.getName());

   public InDashDisplayView(final InDashDisplayController inDashDisplayController,
                            final BMSModel bmsModel,
                            final BMSView bmsView)
      {
      this.setBackground(Color.WHITE);

      final JButton quitButton = SwingUtils.createButton(RESOURCES.getString("label.quit"), true);

      quitButton.addActionListener(
            new ButtonTimeConsumingAction(this, quitButton)
            {
            protected Object executeTimeConsumingAction()
               {
               inDashDisplayController.shutdown();
               return null;
               }
            });

      /*
      quitButton

      bmsView.getLLIMSetGauge()
      bmsView.getHLIMSetGauge()

      bmsView.getOverTemperatureGauge()
      bmsView.getUnderVoltageGauge()
      bmsView.getOverVoltageGauge()
      bmsView.getChargeOvercurrentGauge()
      bmsView.getDischargeOvercurrentGauge()
      bmsView.getCommunicationFaultWithBankOrCellGauge()
      bmsView.getInterlockTrippedGauge()

      ---
      bmsView.getPackTotalVoltageGauge()
      bmsView.getLoadCurrentAmpsGauge()

      */

      // register a connection state listener for the BMS model so we can display connection status
      final JLabel bmsConnectionState = SwingUtils.createLabel(RESOURCES.getString("label.bms"));
      bmsConnectionState.setForeground(UserInterfaceConstants.RED);
      bmsConnectionState.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.BLACK),
                                                                      BorderFactory.createEmptyBorder(3, 3, 3, 3)));
      bmsModel.addStreamingSerialPortDeviceConnectionStateListener(new DeviceConnectionStateListener(bmsConnectionState));

      // create the dials panel components
      final Meter batteryVoltageMeter = bmsView.getBatteryVoltageMeter();
      final Meter batteryCurrentMeter = bmsView.getBatteryCurrentMeter();
      final Component horizontalSpacer1 = SwingUtils.createRigidSpacer(10);
      final Component horizontalSpacer2 = SwingUtils.createRigidSpacer(10);
      final Component horizontalSpacer3 = SwingUtils.createRigidSpacer(10);

      // create the dials panel and layout its components
      final JPanel dialsPanel = new JPanel();
      final GroupLayout dialsPanelLayout = new GroupLayout(dialsPanel);
      dialsPanel.setBackground(Color.WHITE);
      dialsPanel.setLayout(dialsPanelLayout);
      dialsPanelLayout.setHorizontalGroup(
            dialsPanelLayout.createParallelGroup()
                  .addGroup(dialsPanelLayout.createSequentialGroup()
                                  .addComponent(horizontalSpacer1)
                                  .addComponent(batteryVoltageMeter)
                                  .addComponent(horizontalSpacer2)
                                  .addComponent(batteryCurrentMeter)
                                  .addComponent(horizontalSpacer3)
                  )
      );
      dialsPanelLayout.setVerticalGroup(
            dialsPanelLayout.createSequentialGroup()
                  .addGroup(dialsPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                  .addComponent(horizontalSpacer1)
                                  .addComponent(batteryVoltageMeter)
                                  .addComponent(horizontalSpacer2)
                                  .addComponent(batteryCurrentMeter)
                                  .addComponent(horizontalSpacer3)
                  )
      );

      // create the details panel and layout its components
      final JPanel detailsPanel = new JPanel();
      detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
      detailsPanel.setBackground(Color.WHITE);

      final JTable table = new JTable(bmsView.getBatteriesTableModel());
      table.setBackground(Color.WHITE);
      table.setCellSelectionEnabled(false);
      table.setColumnSelectionAllowed(false);
      table.setRowSelectionAllowed(true);
      table.setPreferredScrollableViewportSize(new Dimension(700, 350));
      table.setFillsViewportHeight(true);
      table.setAutoCreateRowSorter(true);

      final JPanel batteryVoltageGauges = new JPanel();
      batteryVoltageGauges.setLayout(new BoxLayout(batteryVoltageGauges, BoxLayout.X_AXIS));
      batteryVoltageGauges.setBackground(Color.WHITE);
      batteryVoltageGauges.add(Box.createGlue());
      batteryVoltageGauges.add(bmsView.getMinimumCellVoltageGauge());
      batteryVoltageGauges.add(Box.createGlue());
      batteryVoltageGauges.add(bmsView.getMaximumCellVoltageGauge());
      batteryVoltageGauges.add(Box.createGlue());
      batteryVoltageGauges.add(bmsView.getAverageCellVoltageGauge());
      batteryVoltageGauges.add(Box.createGlue());
      batteryVoltageGauges.add(bmsView.getPackTotalVoltageGauge());
      batteryVoltageGauges.add(Box.createGlue());

      final JPanel batteryTemperatureGauges = new JPanel();
      batteryTemperatureGauges.setLayout(new BoxLayout(batteryTemperatureGauges, BoxLayout.X_AXIS));
      batteryTemperatureGauges.setBackground(Color.WHITE);
      batteryTemperatureGauges.add(Box.createGlue());
      batteryTemperatureGauges.add(bmsView.getMinimumCellTempGauge());
      batteryTemperatureGauges.add(Box.createGlue());
      batteryTemperatureGauges.add(bmsView.getMaximumCellTempGauge());
      batteryTemperatureGauges.add(Box.createGlue());
      batteryTemperatureGauges.add(bmsView.getAverageCellTempGauge());
      batteryTemperatureGauges.add(Box.createGlue());

      final Border voltageGaugesBorder = BorderFactory.createTitledBorder(RESOURCES.getString("label.group.battery-voltages"));
      batteryVoltageGauges.setBorder(voltageGaugesBorder);

      final Border temperatureGaugesBorder = BorderFactory.createTitledBorder(RESOURCES.getString("label.group.battery-temperatures"));
      batteryTemperatureGauges.setBorder(temperatureGaugesBorder);

      final JPanel batteryGauges = new JPanel();
      batteryGauges.setLayout(new BoxLayout(batteryGauges, BoxLayout.X_AXIS));
      batteryGauges.setBackground(Color.WHITE);
      batteryGauges.add(Box.createGlue());
      batteryGauges.add(batteryVoltageGauges);
      batteryGauges.add(Box.createGlue());
      batteryGauges.add(batteryTemperatureGauges);
      batteryGauges.add(Box.createGlue());

      final JScrollPane scrollPane = new JScrollPane(table);
      scrollPane.setBackground(Color.WHITE);
      detailsPanel.add(scrollPane);
      detailsPanel.add(Box.createGlue());
      detailsPanel.add(batteryGauges);
      detailsPanel.add(Box.createGlue());

      // create this panel's components and layout
      final JTabbedPane tabbedPane = new JTabbedPane();
      tabbedPane.setBackground(Color.WHITE);
      tabbedPane.addTab(RESOURCES.getString("label.dials-tab"), dialsPanel);
      tabbedPane.addTab(RESOURCES.getString("label.details-tab"), detailsPanel);

      final JPanel faultStatusPanel = bmsView.getFaultStatusPanel();

      this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
      this.add(tabbedPane);
      this.add(Box.createGlue());
      this.add(faultStatusPanel);
      this.add(SwingUtils.createRigidSpacer(10));
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
