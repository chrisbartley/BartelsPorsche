package org.chargecar.serial.streaming;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import edu.cmu.ri.createlab.util.mvc.Model;
import edu.cmu.ri.createlab.util.thread.DaemonThreadFactory;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
public abstract class StreamingSerialPortDeviceModel<T, U> extends Model<T, U> implements StreamingSerialPortDeviceConnectionStatePublisher,
                                                                                          StreamingSerialPortDeviceReadingStatePublisher
   {
   private final List<StreamingSerialPortDeviceConnectionStateListener> connectionStateListeners = new ArrayList<StreamingSerialPortDeviceConnectionStateListener>();
   private final List<StreamingSerialPortDeviceReadingStateListener> readingStateListeners = new ArrayList<StreamingSerialPortDeviceReadingStateListener>();
   private final ExecutorService executorService = Executors.newSingleThreadExecutor(new DaemonThreadFactory(this.getClass().getSimpleName()));
   private boolean isConnected = false;
   private boolean isReading = false;

   public final synchronized void addStreamingSerialPortDeviceConnectionStateListener(final StreamingSerialPortDeviceConnectionStateListener listener)
      {
      if (listener != null)
         {
         connectionStateListeners.add(listener);
         }
      }

   public final synchronized void removeStreamingSerialPortDeviceConnectionStateListener(final StreamingSerialPortDeviceConnectionStateListener listener)
      {
      if (listener != null)
         {
         connectionStateListeners.remove(listener);
         }
      }

   public final synchronized void addStreamingSerialPortDeviceReadingStateListener(final StreamingSerialPortDeviceReadingStateListener listener)
      {
      if (listener != null)
         {
         readingStateListeners.add(listener);
         }
      }

   public final synchronized void removeStreamingSerialPortDeviceReadingStateListener(final StreamingSerialPortDeviceReadingStateListener listener)
      {
      if (listener != null)
         {
         readingStateListeners.remove(listener);
         }
      }

   /**
    * Publishes the connection state to all registered {@link StreamingSerialPortDeviceConnectionStateListener}s.  Publication is
    * performed in a separate thread so that control can quickly be returned to the caller.
    */
   public final synchronized void setConnectionState(final boolean isConnected)
      {
      this.isConnected = isConnected;
      if (!connectionStateListeners.isEmpty())
         {
         executorService.execute(
               new Runnable()
               {
               public void run()
                  {
                  for (final StreamingSerialPortDeviceConnectionStateListener listener : connectionStateListeners)
                     {
                     listener.handleConnectionStateChange(isConnected);
                     }
                  }
               });
         }
      }

   public final synchronized boolean isConnected()
      {
      return isConnected;
      }

   /**
    * Publishes the reading state to all registered {@link StreamingSerialPortDeviceReadingStateListener}s.  Publication is
    * performed in a separate thread so that control can quickly be returned to the caller.
    */
   public final synchronized void setReadingState(final boolean isReading)
      {
      this.isReading = isReading;

      if (!readingStateListeners.isEmpty())
         {
         executorService.execute(
               new Runnable()
               {
               public void run()
                  {
                  for (final StreamingSerialPortDeviceReadingStateListener listener : readingStateListeners)
                     {
                     listener.handleReadingStateChange(isReading);
                     }
                  }
               });
         }
      }

   public final synchronized boolean isReading()
      {
      return isReading;
      }
   }
