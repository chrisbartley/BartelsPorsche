package com.chrisbartley.bartels.porsche;

import java.awt.Color;
import java.awt.Component;
import java.util.PropertyResourceBundle;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
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
public final class InDashDisplayView extends JPanel
   {
   private static final Logger LOG = Logger.getLogger(InDashDisplayView.class);
   private static final Logger DATA_LOG = Logger.getLogger("DataLog");

   private static final PropertyResourceBundle RESOURCES = (PropertyResourceBundle)PropertyResourceBundle.getBundle(InDashDisplayView.class.getName());

   public InDashDisplayView(final InDashDisplayController inDashDisplayController,
                            final BMSController bmsController,
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

      bmsView.getFaultStatusPanel()

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
      final JLabel bmsConnectionState = SwingUtils.createLabel(RESOURCES.getString("label.bms"));

      bmsConnectionState.setForeground(UserInterfaceConstants.RED);

      bmsConnectionState.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.BLACK),
                                                                      BorderFactory.createEmptyBorder(3, 3, 3, 3)));

      final Component horizontalSpacer1 = SwingUtils.createRigidSpacer(10);
      final Component horizontalSpacer2 = SwingUtils.createRigidSpacer(10);
      final Component horizontalSpacer3 = SwingUtils.createRigidSpacer(10);
      final Component verticalSpacer = SwingUtils.createRigidSpacer(10);

      final JPanel grid = new JPanel();
      final GroupLayout layout = new GroupLayout(grid);
      grid.setBackground(Color.WHITE);
      grid.setLayout(layout);
      layout.setHorizontalGroup(
            layout.createParallelGroup()
                  .addComponent(bmsView.getFaultStatusPanel())
                  .addGroup(layout.createSequentialGroup()
                                  .addComponent(horizontalSpacer1)
                                  .addComponent(bmsView.getBatteryVoltageMeter())
                                  .addComponent(horizontalSpacer2)
                                  .addComponent(bmsView.getBatteryCurrentMeter())
                                  .addComponent(horizontalSpacer3)
                  )
                  .addComponent(verticalSpacer)
      );
      layout.setVerticalGroup(
            layout.createSequentialGroup()
                  .addComponent(bmsView.getFaultStatusPanel())
                  .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                  .addComponent(horizontalSpacer1)
                                  .addComponent(bmsView.getBatteryVoltageMeter())
                                  .addComponent(horizontalSpacer2)
                                  .addComponent(bmsView.getBatteryCurrentMeter())
                                  .addComponent(horizontalSpacer3)
                  )
                  .addComponent(verticalSpacer)
      );

      this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
      this.add(grid);

      // register self as a connection state listener for the various models so we can display connection status
      bmsModel.addStreamingSerialPortDeviceConnectionStateListener(new DeviceConnectionStateListener(bmsConnectionState));
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
