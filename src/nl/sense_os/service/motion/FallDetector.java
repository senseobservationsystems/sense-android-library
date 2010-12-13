/* 
 * Author Ted
 * 
 * This Fall detection class is based on the fall detection algorithm proposed on:
 * http://www.ecnmag.com/Articles/2009/12/human-fall-detection/
 * 
 */
package nl.sense_os.service.motion;

import android.util.Log;

public class FallDetector 
{
	private class Interrupt
	{
		boolean FREE_FALL		= false;		
		boolean ACTIVITY		= false;
		boolean INACTIVITY		= false;
		@SuppressWarnings("unused")
        boolean BASELINE		= false;
		boolean FALL			= false;
		float	stopFreeFall 	= 0;
		float	stopActivity	= 0;
		@SuppressWarnings("unused")
        float	stopInactivity	= 0;		
	}
	
	private Interrupt interrupt;
	private long startInterrupt	= 0;
	private float G				= 9.81F;
	private float THRESH_FF 	= 0.6F*G;	// Threshold acceleration for a free fall
	private float TIME_FF		= 60F;		// Time in msec of the free fall
	private float TIME_FF_DEMO	= 200F;		// Time in msec of the free fall
	private float THRESH_ACT	= 2.0F*G;	// Threshold for the activity
	private float TIME_FF_ACT	= 200F;		// Time between a free fall and activity (msec) 200msec was standard 300 for low sampling	
	private float THRESH_INACT	= 1.3F*G; 	// Threshold for inactivity, default was 0.1875F*G;
	private float TIME_INACT	= 2000F;	// Time of inactivity (msec)
	private float TIME_ACT_INACT= 3500;		// Time between an activity and inactivity
	@SuppressWarnings("unused")
    private float THRESH_INITIAL= 0.7F*G;	// Threshold for the difference between the initial status and after inactivity
	@SuppressWarnings("unused")
    private long  time 			= 0;		// Time of last function call
	public boolean demo			= true;		// For demoing only the free fall is used
	
	private boolean useInactivity = false;  // Use the inactivity property to determine a fall
	public FallDetector()
	{
		interrupt = new Interrupt();
	}

	public boolean fallDetected(float accVecSum)
	{	
		//Log.d("Fall detection:", "time:"+(System.currentTimeMillis()-time));
		time = System.currentTimeMillis();
		
		
		if(interrupt.FALL || (demo && interrupt.FREE_FALL))
			reset();

		freeFall(accVecSum);

		if(demo)
		{
			if(interrupt.FREE_FALL)
			{
				reset();				
				return true;
			}
		}
		else
		{
			activity(accVecSum);

			if(useInactivity)
			{
				if(!interrupt.INACTIVITY)		
					inactivity(accVecSum);
			}
			else
				interrupt.FALL = interrupt.ACTIVITY;

			if(interrupt.FALL)
			{
				reset();
				return true;
			}
		}
		return false;
	}
	
	public void freeFall(float accVecSum)
	{
		if(accVecSum < THRESH_FF)
		{
			if(startInterrupt == 0)			
				startInterrupt = System.currentTimeMillis();			
			else if ((System.currentTimeMillis()-startInterrupt > TIME_FF && !demo) ||
					(System.currentTimeMillis()-startInterrupt > TIME_FF_DEMO && demo))
			{
				Log.d("Fall detection:", "FF time:"+(System.currentTimeMillis()-startInterrupt));
				interrupt.FREE_FALL = true;
			}
		}
		else if (interrupt.FREE_FALL)
		{
			interrupt.stopFreeFall = System.currentTimeMillis();
			interrupt.FREE_FALL = false;
			startInterrupt = 0;
		}
		else		
			startInterrupt = 0;		
		
		if(interrupt.FREE_FALL)
			Log.d("Fall detection:", "FALL!!!");
	}
	
	public void activity(float accVecSum)
	{
		if(interrupt.stopFreeFall == 0)
			return;
		
		// If the threshold for a activity is reached
		// and if it is within the time frame after a fall
		// then there is activity
		if(accVecSum >= THRESH_ACT)
		{
			if(System.currentTimeMillis()-interrupt.stopFreeFall < TIME_FF_ACT)
			{
				startInterrupt = System.currentTimeMillis();
				interrupt.ACTIVITY = true;		
			}			
		}
		// If the activity is over
		// note the stop time of this activity		
		else if(interrupt.ACTIVITY)
		{
			interrupt.stopActivity = System.currentTimeMillis();
			startInterrupt = 0;
			interrupt.ACTIVITY = false;
		}
		
		// The time for an activity has passed and there was never an activity interrupt
		// reset;
		if(System.currentTimeMillis()-interrupt.stopFreeFall > TIME_FF_ACT)		
			if(interrupt.stopActivity == 0)
				reset();
		
		if(interrupt.ACTIVITY)
			Log.d("Fall detection:", "Activity!!!");
	}
	
	public void inactivity(float accVecSum)
	{
		if(interrupt.stopActivity == 0)
			return;
		
		if(accVecSum < THRESH_INACT)
		{
			if(System.currentTimeMillis()-interrupt.stopActivity < TIME_ACT_INACT)			
				if(startInterrupt == 0)
					startInterrupt = System.currentTimeMillis();
			
			if(startInterrupt != 0 && System.currentTimeMillis()-startInterrupt > TIME_INACT)			
				interrupt.INACTIVITY = true;			
		}
		else if(startInterrupt != 0 && !interrupt.INACTIVITY)
			reset();
			
		if(System.currentTimeMillis()-interrupt.stopActivity >= TIME_ACT_INACT && startInterrupt == 0)
			reset();
		
		interrupt.FALL = interrupt.INACTIVITY;
		
		if(interrupt.INACTIVITY)
			Log.d("Fall detection:", "Inactivity!!!");
	}
	
	private void reset()
	{
		interrupt = new Interrupt();
		startInterrupt = 0;
	}
}
