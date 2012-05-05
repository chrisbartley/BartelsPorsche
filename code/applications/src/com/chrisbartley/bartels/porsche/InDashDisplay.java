package com.chrisbartley.bartels.porsche;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.PropertyResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.Box;
import javax.swing.GroupLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import edu.cmu.ri.createlab.userinterface.util.DialogHelper;
import edu.cmu.ri.createlab.util.runtime.LifecycleManager;
import edu.cmu.ri.createlab.util.thread.DaemonThreadFactory;
import org.apache.log4j.Logger;
import org.chargecar.bms.BMSController;
import org.chargecar.bms.BMSModel;
import org.chargecar.bms.BMSView;
import org.chargecar.serial.streaming.StreamingSerialPortDeviceConnectionStateListener;
import org.chargecar.serial.streaming.StreamingSerialPortDeviceController;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
@SuppressWarnings({"UseOfSystemOutOrSystemErr"})
public final class InDashDisplay
   {
   private static final Logger LOG = Logger.getLogger(InDashDisplay.class);
   private static final PropertyResourceBundle RESOURCES = (PropertyResourceBundle)PropertyResourceBundle.getBundle(InDashDisplay.class.getName());
   private static final String APPLICATION_NAME = RESOURCES.getString("application.name");
   private static final Dimension PREFERRED_WINDOW_SIZE = new Dimension(800, 600);

   public static void main(final String[] args)
      {
      final Map<String, String> deviceToSerialPortNameMap = new HashMap<String, String>(4);

      for (final String arg : args)
         {
         final String[] keyValue = arg.split("=");
         if (keyValue.length == 2)
            {
            LOG.debug("Associating [" + keyValue[0] + "] with serial port [" + keyValue[1] + "]");
            deviceToSerialPortNameMap.put(keyValue[0].toLowerCase(), keyValue[1]);
            }
         else
            {
            LOG.info("Ignoring unexpected switch [" + arg + "]");
            }
         }

      SwingUtilities.invokeLater(
            new Runnable()
            {
            public void run()
               {
               new InDashDisplay(deviceToSerialPortNameMap);
               }
            });
      }

   private InDashDisplay(final Map<String, String> deviceToSerialPortNameMap)
      {
      // create and configure the GUI
      final JPanel panel = new JPanel();
      panel.setBackground(Color.WHITE);
      final JFrame jFrame = new JFrame(APPLICATION_NAME);
      jFrame.setContentPane(panel);

      panel.setPreferredSize(PREFERRED_WINDOW_SIZE);// changed it to preferredSize, Thanks!

      // create the model
      final BMSModel bmsModel = new BMSModel(BartelsPorscheConstants.NUM_BATTERIES);

      // create the controller
      final BMSController bmsController = BMSController.create(deviceToSerialPortNameMap.get("bms"), bmsModel);

      final LifecycleManager lifecycleManager = new MyLifecycleManager(jFrame, bmsController);

      final InDashDisplayController inDashDisplayController = new InDashDisplayController(lifecycleManager);

      // create the views
      final BMSView bmsView = new BMSView(bmsController, bmsModel);
      final InDashDisplayView inDashDisplayView = new InDashDisplayView(inDashDisplayController,
                                                                        bmsModel,
                                                                        bmsView);

      // add the various views as data event listeners to the models
      bmsModel.addEventListener(bmsView);

      // add the various views as connection state listeners to the models
      bmsModel.addStreamingSerialPortDeviceConnectionStateListener(bmsView);

      // add the various views as reading state listeners to the models
      bmsModel.addStreamingSerialPortDeviceReadingStateListener(bmsView);

      // add a listener to each model which starts the reading upon connection establishment
      bmsModel.addStreamingSerialPortDeviceConnectionStateListener(
            new StreamingSerialPortDeviceConnectionStateListener()
            {
            public void handleConnectionStateChange(final boolean isConnected)
               {
               if (isConnected)
                  {
                  bmsController.startReading();
                  }
               }
            });

      // setup the layout
      final GroupLayout layout = new GroupLayout(panel);
      final Component leftGlue = Box.createGlue();
      final Component rightGlue = Box.createGlue();
      panel.setLayout(layout);
      layout.setHorizontalGroup(
            layout.createSequentialGroup()
                  .addComponent(leftGlue)
                  .addComponent(inDashDisplayView)
                  .addComponent(rightGlue)
      );
      layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                  .addComponent(leftGlue)
                  .addComponent(inDashDisplayView)
                  .addComponent(rightGlue)
      );
      jFrame.pack();

      // set various properties for the JFrame
      jFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
      jFrame.setBackground(Color.WHITE);
      jFrame.setResizable(true);
      jFrame.addWindowListener(
            new WindowAdapter()
            {
            public void windowOpened(final WindowEvent e)
               {
               LOG.debug("InDashDisplay.windowOpened()");
               lifecycleManager.startup();
               }

            public void windowClosing(final WindowEvent event)
               {
               lifecycleManager.shutdown();
               }
            });

      jFrame.setVisible(true);
      }

   private static class MyLifecycleManager implements LifecycleManager
      {
      private final Runnable startupRunnable;
      private final Runnable shutdownRunnable;
      private final JFrame jFrame;
      private final ExecutorService executor = Executors.newCachedThreadPool(new DaemonThreadFactory("MyLifecycleManager.executor"));

      private MyLifecycleManager(final JFrame jFrame, final BMSController bmsController)
         {
         this.jFrame = jFrame;
         startupRunnable =
               new Runnable()
               {
               private void connect(final String deviceName, final StreamingSerialPortDeviceController controller)
                  {
                  if (controller == null)
                     {
                     LOG.info("InDashDisplay$MyLifecycleManager.run(): Controller for the " + deviceName + " given to the LifecycleManager constructor was null, so data won't be read.");
                     }
                  else
                     {
                     executor.submit(
                           new Runnable()
                           {
                           public void run()
                              {
                              LOG.info("InDashDisplay$MyLifecycleManager.run(): Attempting to establish a connection to the " + deviceName + "...");
                              controller.connect();
                              }
                           });
                     }
                  }

               public void run()
                  {
                  connect("BMS", bmsController);
                  }
               };

         shutdownRunnable =
               new Runnable()
               {
               private void disconnect(final String deviceName, final StreamingSerialPortDeviceController controller)
                  {
                  if (controller == null)
                     {
                     LOG.info("InDashDisplay$MyLifecycleManager.run(): Controller for the " + deviceName + " given to the LifecycleManager constructor was null, so we won't try to shut it down.");
                     }
                  else
                     {
                     executor.submit(
                           new Runnable()
                           {
                           public void run()
                              {
                              LOG.info("InDashDisplay$MyLifecycleManager.run(): Disconnecting from the " + deviceName + "...");
                              controller.disconnect();
                              }
                           });
                     }
                  }

               public void run()
                  {
                  disconnect("BMS", bmsController);

                  System.exit(0);
                  }
               };
         }

      public void startup()
         {
         LOG.debug("LifecycleManager.startup()");

         run(startupRunnable);
         }

      public void shutdown()
         {
         LOG.debug("LifecycleManager.shutdown()");

         // ask if the user really wants to exit
         if (DialogHelper.showYesNoDialog(RESOURCES.getString("dialog.title.exit-confirmation"),
                                          RESOURCES.getString("dialog.message.exit-confirmation"),
                                          jFrame))
            {
            run(shutdownRunnable);
            }
         }

      private void run(final Runnable runnable)
         {
         if (SwingUtilities.isEventDispatchThread())
            {
            final SwingWorker sw =
                  new SwingWorker<Object, Object>()
                  {
                  @Override
                  protected Object doInBackground() throws Exception
                     {
                     runnable.run();
                     return null;
                     }
                  };
            sw.execute();
            }
         else
            {
            runnable.run();
            }
         }
      }
   }
