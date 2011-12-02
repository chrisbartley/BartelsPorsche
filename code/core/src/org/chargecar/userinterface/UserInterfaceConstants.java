package org.chargecar.userinterface;

import java.awt.Color;

/**
 * <p>
 * <code>UserInterfaceConstants</code> defines various constants common to ChargeCar user interfaces.
 * </p>
 *
 * @author Chris Bartley (bartley@cmu.edu)
 */
public final class UserInterfaceConstants
   {
   public static final String UNKNOWN_VALUE = "?";
   public static final Color GREEN = new Color(0, 170, 0);
   public static final Color RED = new Color(170, 0, 0);

   public static final Color DEFAULT_METER_COLOR = new Color(200, 200, 220);
   public static final Color METER_WARNING_COLOR = new Color(255, 150, 150);

   private UserInterfaceConstants()
      {
      // private to prevent instantiation
      }
   }
