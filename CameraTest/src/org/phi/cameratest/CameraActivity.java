package org.phi.cameratest;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

/**
 * Camera Preview with pie parts overlay.<br/>
 * Resources:<br/>
 * - https://github.com/commonsguy/cw-advandroid/blob/master/Camera/Preview/src/com/commonsware/android/camera/PreviewDemo.java
 * @author paristote
 *
 */
public class CameraActivity extends Activity {
	  public static final String LOGTAG="CAM";
	
	  private RelativeLayout optionsLayout; // the options layout
	  private ImageView circleView; // the view that displays the circle image
	  private SurfaceView preview=null;
	  private SurfaceHolder previewHolder=null;
	  private Camera camera=null;
	  private boolean inPreview=false;
	  private boolean cameraConfigured=false;
	  private boolean optionsHidden=false; // options shown by default
	  private int numberOfParts=3; // 3 parts by default
	  private static int radius=100;
	  /**
	   * Callback methods when certain events affect the SurfacePreview holder.
	   */
	  SurfaceHolder.Callback surfaceCallback=new SurfaceHolder.Callback() {
	    public void surfaceCreated(SurfaceHolder holder) {
	      // no-op -- wait until surfaceChanged()
	    }
	    
	    public void surfaceChanged(SurfaceHolder holder,
	                               int format, int width,
	                               int height) {
	      initPreview(width, height);
	      startPreview();
	    }
	    
	    public void surfaceDestroyed(SurfaceHolder holder) {
	      // no-op
	    }
	  };
	  
	  @Override
	  public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
	    setContentView(R.layout.activity_main);
	    
	    preview=(SurfaceView)findViewById(R.id.surfaceView1);
	    previewHolder=preview.getHolder();
	    previewHolder.addCallback(surfaceCallback); // sets callback methods
	    previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	    
	    optionsLayout = (RelativeLayout)findViewById(R.id.optionsLayout);
	    
	    circleView = (ImageView)findViewById(R.id.imageView1);
	    drawImage(numberOfParts);
	    optionsLayout.setVisibility(View.VISIBLE);
	  }
	  /**
	   * Called when the user taps the screen.<br/>
	   * Hides or displays the overlay options.
	   * @param view
	   */
	  public void showHideOptions(View view) {
		  if (optionsHidden) { // Showing options
			  optionsLayout.setVisibility(View.VISIBLE);
			  optionsHidden = false;
		  } else { // Hiding options
			  optionsLayout.setVisibility(View.INVISIBLE);
			  optionsHidden = true;
		  }
	  }
	  /**
	   * Updates the pie image according to which button was tapped.
	   * @param view
	   */
	  public void changeImage(View view) {
		  int viewId = view.getId();
		switch (viewId) {
		case R.id.circle3:
			numberOfParts = 3;
			break;
		case R.id.circle5:
			numberOfParts = 5;
			break;
		case R.id.circle7:
			numberOfParts = 7;
			break;
		case R.id.circle9:
			numberOfParts = 9;
			break;
		default:
			break;
		}
		drawImage(numberOfParts);
	  }
	  /**
	   * Draws the circle with <code>n</code> parts.
	   * @param n The number of parts to draw on the pie.
	   */
	  private void drawImage(int n) {
		  Display display = getWindowManager().getDefaultDisplay();
		  int h = display.getHeight();
		  int w = display.getWidth();
		  Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
		  Canvas c = new Canvas(bmp);
		  Paint paint = new Paint();
		  paint.setColor(Color.GREEN);
		  paint.setStrokeWidth(2);
		  paint.setStyle(Paint.Style.STROKE);
		  paint.setAntiAlias(true);
		  c.drawCircle(w/2, h/2, radius, paint);
		  Point center = new Point(w/2, h/2);
		  Point end = new Point(w/2, (h/2)-radius);
		  float angle = 360/n;
		  for (int i=0;i<n;i++) {
			  c.drawLine(center.x, center.y, end.x, end.y, paint);
			  end = calculateNewEnd(center, end, angle);
		  }
		  circleView.setImageDrawable(new BitmapDrawable(bmp));
		  showHideOptions(null);
	  }
	  /**
	   * Calculate the position of the point at the end of a line after a certain rotation around an origin point.<br/>
	   * - line starts from <code>origin</code><br/>
	   * - line ends at <code>oldEnd</code><br/>
	   * - rotate around <code>origin</code><br/>
	   * - rotate of <code>angle</code> degrees
	   * @param origin The origin of the line.
	   * @param oldEnd The end of the line.
	   * @param angle The angle to rotate.
	   * @return The new end point of the line, after the rotation.
	   */
	  private Point calculateNewEnd(Point origin, Point oldEnd, float angle) {
		  double cos = Math.cos(angle*Math.PI/180);
		  double sin = Math.sin(angle*Math.PI/180);
		  //       x' = (x    -    xc)  *  cos0  - (y  -   yc)  * sin0     +xc
		  double newx = ((oldEnd.x-origin.x)*cos)-((oldEnd.y-origin.y)*sin)+origin.x;
		  //       y' = (y    -    yc)  *  cos0  + (x  -   xc)  * sin0     +yc
		  double newy = ((oldEnd.y-origin.y)*cos)+((oldEnd.x-origin.x)*sin)+origin.y;
		  return new Point((int)newx, (int)newy);
	  }
	  
	  @Override
	  public void onResume() {
	    super.onResume();
	    
	    camera=Camera.open();
	    startPreview();
	  }
	    
	  @Override
	  public void onPause() {
	    if (inPreview) {
	      camera.stopPreview();
	    }
	    
	    camera.release();
	    camera=null;
	    inPreview=false;
	          
	    super.onPause();
	  }
	  /**
	   * Calculate the image size that best fits the surface size.
	   * @param width The surface width.
	   * @param height The surface height.
	   * @param parameters The device's camera parameters.
	   * @return the best Camera.Size object for this screen.
	   */
	  private Camera.Size getBestPreviewSize(int width, int height,
	                                         Camera.Parameters parameters) {
	    Camera.Size result=null;
	    
	    for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
	      if (size.width<=width && size.height<=height) {
	        if (result==null) {
	          result=size;
	        }
	        else {
	          int resultArea=result.width*result.height;
	          int newArea=size.width*size.height;
	          
	          if (newArea>resultArea) {
	            result=size;
	          }
	        }
	      }
	    }
	    
	    return(result);
	  }
	  /**
	   * Initialize the camera preview with the following parameters:<br/>
	   * - the SurfaceView that displays the camera image<br/>
	   * - the best size to fit in the device's screen
	   * @param width The surface width.
	   * @param height The surface height.
	   */
	  private void initPreview(int width, int height) {
	    if (camera!=null && previewHolder.getSurface()!=null) {
	      try {
	        camera.setPreviewDisplay(previewHolder);
	      }
	      catch (Throwable t) {
	        Log.e(LOGTAG,
	              "Exception in setPreviewDisplay()", t);
	        Toast
	          .makeText(CameraActivity.this, t.getMessage(), Toast.LENGTH_LONG)
	          .show();
	      }

	      if (!cameraConfigured) {
	        Camera.Parameters parameters=camera.getParameters();
	        Camera.Size size=getBestPreviewSize(width, height,
	                                            parameters);
	        
	        if (size!=null) {
	          parameters.setPreviewSize(size.width, size.height);
	          camera.setParameters(parameters);
	          cameraConfigured=true;
	        }
	      }
	    }
	  }
	  /**
	   * Start the preview of the camera on the surface view.
	   */
	  private void startPreview() {
	    if (cameraConfigured && camera!=null) {
	      camera.startPreview();
	      inPreview=true;
	    }
	  }
}

