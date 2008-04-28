package geomerative ;
import processing.core.*;

public class RCommand
{
  /**
   * @invisible
   */
  public int type = RGeomElem.COMMAND;
  
  public RPoint[] controlPoints;
  public RPoint startPoint;
  public RPoint endPoint;
  int commandType;
  
  RPoint[] curvePoints;
  
  /**
   * @invisible
   * */
  public static final int LINETO = 0;
  /**
   * @invisible
   * */
  public static final int QUADBEZIERTO = 1;
  /**
   * @invisible
   * */
  public static final int CUBICBEZIERTO = 2;
  
  /**
   * @invisible
   * */
  public static final int ADAPTATIVE = 0;
  /**
   * @invisible
   * */
  public static final int UNIFORMLENGTH = 1;
  /**
   * @invisible
   * */
  public static final int UNIFORMSTEP = 2;
  
  public static int segmentType = UNIFORMLENGTH;
  
  /* Parameters for ADAPTATIVE (dependent of the PGraphics on which drawing) */
  static final int segmentRecursionLimit = 32;
  static final float segmentDistanceEpsilon = 1.192092896e-07F;
  static final float segmentCollinearityEpsilon = 1.192092896e-07F;
  static final float segmentAngleTolEpsilon = 0.01F;
  
  static float segmentGfxStrokeWeight = 1.0F;
  static float segmentGfxScale = 1.0F;
  static float segmentApproxScale = 1.0F;
  static float segmentDistTolSqr = 0.25F;
  static float segmentDistTolMnhttn = 4.0F;
  public static float segmentAngleTol = 0.0F;
  static float segmentCuspLimit = 0.0F;
  
  /* Parameters for UNIFORMLENGTH (dependent of the PGraphics on which drawing) */
  static float segmentLength = 4.0F;
  static float segmentOffset = 0.0F;
  static float segmentAccOffset = 0.0F;
  
  /* Parameters for UNIFORMSTEP */
  static int segmentSteps = 0;
  static boolean segmentLines = false;
  
  
  static RCommand createLine(RPoint start, RPoint end){
    RCommand result = new RCommand();
    result.startPoint = start;
    result.endPoint = end;
    result.commandType = LINETO;
    return result;
  }
  
  static RCommand createLine(float startx, float starty, float endx, float endy){
    return createLine(new RPoint(startx,starty), new RPoint(endx,endy));
  }
  
  static RCommand createBezier3(RPoint start, RPoint cp1, RPoint end){
    RCommand result = new RCommand();
    result.startPoint = start;
    result.append(cp1);
    result.endPoint = end;
    result.commandType = QUADBEZIERTO;
    return result;
  }
  
  static RCommand createBezier3(float startx, float starty, float cp1x, float cp1y, float endx, float endy){
    return createBezier3(new RPoint(startx,starty), new RPoint(cp1x,cp1y), new RPoint(endx,endy));
  }
  
  static RCommand createBezier4(RPoint start, RPoint cp1, RPoint cp2, RPoint end){
    RCommand result = new RCommand();
    result.startPoint = start;
    result.append(cp1);
    result.append(cp2);
    result.endPoint = end;
    result.commandType = CUBICBEZIERTO;
    return result;
  }
  
  static RCommand createBezier4(float startx, float starty, float cp1x, float cp1y, float cp2x, float cp2y, float endx, float endy){
    return createBezier4(new RPoint(startx,starty), new RPoint(cp1x,cp1y), new RPoint(cp2x,cp2y), new RPoint(endx,endy));
  }
  
  /**
   * Use this constructor to make a copy of another RCommand object.  This can be useful when wanting to transform one but at the same time keep the original.
   * @param RCommand c, the object of which to make the copy
   * @invisible
   */
  public RCommand(){
    controlPoints = null;
  }
  
  public RCommand(RCommand c){
    this.startPoint = new RPoint(c.startPoint);
    for(int i=0;i<c.countControlPoints();i++){
      this.append(new RPoint(c.controlPoints[i]));
    }
    this.endPoint = new RPoint(c.endPoint);
    this.commandType = c.commandType;
  }
  
  public RCommand(RCommand c, RPoint sp){
    this.startPoint = sp;
    for(int i=0;i<c.countControlPoints();i++){
      this.append(new RPoint(c.controlPoints[i]));
    }
    this.endPoint = new RPoint(c.endPoint);
    this.commandType = c.commandType;
  }
  
  /**
   * Use this to set the segmentator type.  ADAPTATIVE segmentator minimizes the number of segments avoiding perceptual artifacts like angles or cusps.  Use this in order to have Polygons and Meshes with the fewest possible vertices.  This can be useful when using or drawing a lot the same Polygon or Mesh deriving from this Shape.  UNIFORMLENGTH segmentator is the slowest segmentator and it segments the curve on segments of equal length.  This can be useful for very specific applications when for example drawing incrementaly a shape with a uniform speed.  UNIFORMSTEP segmentator is the fastest segmentator and it segments the curve based on a constant value of the step of the curve parameter, or on the number of segments wanted.  This can be useful when segmpointsentating very often a Shape or when we know the amount of segments necessary for our specific application.
   * @eexample setSegment
   * */
  public static void setSegmentator(int segmentatorType){
    segmentType = segmentatorType;
  }
  
  /**
   * Use this to set the segmentator graphic context.
   * @eexample setSegmentGraphic
   * @param PGraphics g, graphics object too which to adapt the segmentation of the command.
   * */
  public static void setSegmentGraphic(PGraphics g){
    // Set the segmentApproxScale from the graphic context g
    segmentApproxScale = 1.0F;
    
    // Set all the gfx-context dependent parameters for all segmentators
    
    segmentDistTolSqr = 0.5F / segmentApproxScale;
    segmentDistTolSqr *= segmentDistTolSqr;
    segmentDistTolMnhttn = 4.0F / segmentApproxScale;
    segmentAngleTol = 0.0F;
    
    if(g.stroke && (g.strokeWeight * segmentApproxScale > 1.0F))
      {
        segmentAngleTol = 0.1F;
      }
  }
  
  /**
   * Use this to set the segmentator angle tolerance for the ADAPTATIVE segmentator and set the segmentator to ADAPTATIVE.
   * @eexample setSegmentAngle
   * @param float segmentAngleTolerance, an angle from 0 to PI/2 it defines the maximum angle between segments.
   * */
  public static void setSegmentAngle(float segmentAngleTolerance){
    //segmentType = ADAPTATIVE;
    
    segmentAngleTol = segmentAngleTolerance;
  }
  
  /**
   * Use this to set the segmentator length for the UNIFORMLENGTH segmentator and set the segmentator to UNIFORMLENGTH.
   * @eexample setSegmentLength
   * @param float segmentLngth, the length of each resulting segment.
   * */
  public static void setSegmentLength(float segmentLngth){
    //segmentType = UNIFORMLENGTH;
    if(segmentLngth>=1){
      segmentLength = segmentLngth;
    }else{
      segmentLength = 4;
    }
  }
  
  /**
   * Use this to set the segmentator offset for the UNIFORMLENGTH segmentator and set the segmentator to UNIFORMLENGTH.
   * @eexample setSegmentOffset
   * @param float segmentOffst, the offset of the first point on the path.
   * */
  public static void setSegmentOffset(float segmentOffst){
    //segmentType = UNIFORMLENGTH;
    if(segmentOffst>=0){
      segmentOffset = segmentOffst;
    }else{
      segmentOffset = 0;
    }
  }
  
  /**
   * Use this to set the segmentator step for the UNIFORMSTEP segmentator and set the segmentator to UNIFORMSTEP.
   * @eexample setSegmentStep
   * @param float segmentStps, if a float from +0.0 to 1.0 is passed it's considered as the step, else it's considered as the number of steps.  When a value of 0.0 is used the steps will be calculated automatically depending on an estimation of the length of the curve.  The special value -1 is the same as 0.0 but also turning of the segmentation of lines (faster segmentation).
   * */
  public static void setSegmentStep(float segmentStps){
    //segmentType = UNIFORMSTEP;
    if(segmentStps == -1F){
      segmentLines=false;
      segmentStps=0F;
    }else{
      segmentLines=true;
    }
    // Set the parameters
    segmentStps = Math.abs(segmentStps);
    if(segmentStps>0.0F && segmentStps<1.0F){
      segmentSteps = (int)(1F/segmentStps);
    }else{
      segmentSteps = (int)segmentStps;
    }
  }
  
  
  
  
  
  /**
   * Use this to return the number of control points of the curve.
   * @eexample countControlPoints
   * @return int, the number of control points.
   * */
  public int countControlPoints(){
    if (controlPoints == null){
      return 0;
    }
    return controlPoints.length;
  }
  
  /**
   * Use this to return the command type.
   * @eexample getCommandType
   * @return int, an integer which can take the following values: RCommand.LINETO, RCommand.QUADBEZIERTO, RCommand.CUBICBEZIERTO.
   * */
  public int getCommandType(){
    return commandType;
  }
  
  /**
   * Use this to return the start point of the curve.
   * @eexample getStartPoint
   * @return RPoint, the start point of the curve.
   * @invisible
   * */
  RPoint getStartPoint(){
    return startPoint;
  }
  
  /**
   * Use this to return the end point of the curve.
   * @eexample getEndPoint
   * @return RPoint, the end point of the curve.
   * @invisible
   * */
  RPoint getEndPoint(){
    return endPoint;
  }
  
  /**
   * Use this to return the control points of the curve.  It returns the points in the way of an array of RPoint.
   * @eexample getControlPoints
   * @return RPoint[], the control points returned in an array.
   * @invisible
   * */
  RPoint[] getControlPoints(){
    return controlPoints;
  }
  
  /**
   * Use this to return the points on the curve.  It returns the points in the way of an array of RPoint.
   * @eexample getCurvePoints
   * @param segments int, the number of segments in which to divide the curve.
   * @return RPoint[], the vertices returned in an array.
   * */
  public RPoint[] getCurvePoints(){
    RPoint[] result;
    switch(segmentType){
    case ADAPTATIVE:
      switch(commandType){
      case LINETO:
        result = new RPoint[2];
        result[0] = startPoint;
        result[1] = endPoint;
        return result;
      case QUADBEZIERTO:
        quadBezierAdaptative();
        result = curvePoints;
        curvePoints = null;
        return result;
      case CUBICBEZIERTO:
        cubicBezierAdaptative();
        result = curvePoints;
        curvePoints = null;
        return result;
      }
    case UNIFORMLENGTH:
      switch(commandType){
      case LINETO:
        lineUniformLength();
        result = curvePoints;
        curvePoints = null;
        return result;
      case QUADBEZIERTO:
        quadBezierUniformLength();
        result = curvePoints;
        curvePoints = null;
        return result;
      case CUBICBEZIERTO:
        cubicBezierUniformLength();
        result = curvePoints;
        curvePoints = null;
        return result;
      }
    case UNIFORMSTEP:
      switch(commandType){
      case LINETO:
        if(segmentLines){
          lineUniformStep();
          result = curvePoints;
          curvePoints = null;
        }else{
          result = new RPoint[2];
          result[0] = startPoint;
          result[1] = endPoint;
        }
        return result;
      case QUADBEZIERTO:
        quadBezierUniformStep();
        result = curvePoints;
        curvePoints = null;
        return result;
      case CUBICBEZIERTO:
        cubicBezierUniformStep();
        result = curvePoints;
        curvePoints = null;
        return result;
      }
    }
    return null;
  }
  
  /**
   * Use this to return a specific point on the curve.  It returns the RPoint for a given advancement parameter t on the curve.
   * @eexample getCurvePoint
   * @param t float, the parameter of advancement on the curve. t must have values between 0 and 1.
   * @return RPoint, the vertice returned.
   * */
  public RPoint getCurvePoint(float t){
    /* limit the value of t between 0 and 1 */
    t = (t > 1F) ? 1F : t;
    t = (t < 0F) ? 0F : t;
    float ax, bx, cx;
    float ay, by, cy;
    float tSquared, tDoubled, tCubed;
    
    switch(commandType){
    case LINETO:
      float dx = endPoint.x - startPoint.x;
      float dy = endPoint.y - startPoint.y;
      return new RPoint(startPoint.x + dx * t, startPoint.y + dy * t);
      
    case QUADBEZIERTO:
      /* calculate the polynomial coefficients */
      bx = controlPoints[0].x - startPoint.x;
      ax = endPoint.x - controlPoints[0].x - bx;
      by = controlPoints[0].y - startPoint.y;
      ay = endPoint.y - controlPoints[0].y - by;
      
      /* calculate the curve point at parameter value t */
      tSquared = t * t;
      tDoubled = 2F * t;
      return new RPoint((ax * tSquared) + (bx * tDoubled) + startPoint.x, (ay * tSquared) + (by * tDoubled) + startPoint.y);
      
    case CUBICBEZIERTO:
      /* calculate the polynomial coefficients */
      cx = 3F * (controlPoints[0].x - startPoint.x);
      bx = 3F * (controlPoints[1].x - controlPoints[0].x) - cx;
      ax = endPoint.x - startPoint.x - cx - bx;
      cy = 3F * (controlPoints[0].y - startPoint.y);
      by = 3F * (controlPoints[1].y - controlPoints[0].y) - cy;
      ay = endPoint.y - startPoint.y - cy - by;
      
      /* calculate the curve point at parameter value t */
      tSquared = t * t;
      tCubed = tSquared * t;
      return new RPoint((ax * tCubed) + (bx * tSquared) + (cx * t) + startPoint.x, (ay * tCubed) + (by * tSquared) + (cy * t) + startPoint.y);
    }
    
    return new RPoint();
  }
  
  /**
   * Use this to return the tangents on the curve.  It returns the vectors in the form of an array of RPoint.
   * @eexample getCurveTangents
   * @param segments int, the number of segments in which to divide the curve.
   * @return RPoint[], the tangent vectors returned in an array.
   * */
  public RPoint[] getCurveTangents(int segments){
    RPoint[] result;
    switch(commandType){
    case LINETO:
      result = new RPoint[2];
      result[0] = startPoint;
      result[1] = endPoint;
      return result;
    case QUADBEZIERTO:
    case CUBICBEZIERTO:
      result = new RPoint[segments];
      float dt = 1F / segments;
      float t = 0F;
      for(int i=0;i<segments;i++){
        result[i]=getCurveTangent(t);
        t += dt;
      }
      return result;
    }
    return null;
  }
  
  public RPoint[] getCurveTangents(){
    
    return getCurveTangents(100);
  }
  
  /**
   * Use this to return a specific tangent on the curve.  It returns the RPoint representing the tangent vector for a given value of the advancement parameter t on the curve.
   * @eexample getCurveTangent
   * @param t float, the parameter of advancement on the curve. t must have values between 0 and 1.
   * @return RPoint, the vertice returned.
   * */
  public RPoint getCurveTangent(float t){
    /* limit the value of t between 0 and 1 */
    t = (t > 1F) ? 1F : t;
    t = (t < 0F) ? 0F : t;
    
    switch(commandType){
    case LINETO:
      float dx = endPoint.x - startPoint.x;
      float dy = endPoint.y - startPoint.y;
      return new RPoint(dx, dy);
      
    case QUADBEZIERTO:
      /* calculate the curve point at parameter value t */
      float tx = 2F * ((startPoint.x - 2*controlPoints[0].x + endPoint.x) * t + (controlPoints[0].x - startPoint.x));
      float ty = 2F * ((startPoint.y - 2*controlPoints[0].y + endPoint.y) * t + (controlPoints[0].y - startPoint.y));
      float norm = (float)Math.sqrt(tx*tx + ty*ty);
      return new RPoint(tx/norm,ty/norm);
      
    case CUBICBEZIERTO:
      /* calculate the curve point at parameter value t */
      float t2 = t*t;
      float t_1 = 1-t;
      float t_12 = t_1*t_1;
      
      return new RPoint(-3F*t_12*startPoint.x + 3F*(3F*t2 - 4F*t +1F)*controlPoints[0].x + 3F*t*(2F-3F*t)*controlPoints[1].x + 3F*t2*endPoint.x, -3F*t_12*startPoint.y + 3F*(3F*t2 - 4F*t +1F)*controlPoints[0].y + 3F*t*(2F-3F*t)*controlPoints[1].y + 3F*t2*endPoint.y);
    }
    
    return new RPoint();
  }
  
  /**
   * Use this to return arc length of a curve.  It returns the float representing the length given the value of the advancement parameter t on the curve. The current implementation of this function is very slow, not recommended for using during frame draw.
   * @eexample RCommand_getCurveLength
   * @param t float, the parameter of advancement on the curve. t must have values between 0 and 1.
   * @return float, the length returned.
   * @invisible
   * */
  public float getCurveLength(float t){
    float arclength = 0F;
    
    /* limit the value of t between 0 and 1 */
    t = (t > 1F) ? 1F : t;
    t = (t < 0F) ? 0F : t;
    
    switch(commandType){
    case LINETO:
      float dx = endPoint.x - startPoint.x;
      float dy = endPoint.y - startPoint.y;
      float dx2 = dx*dx;
      float dy2 = dy*dy;
      float t2 = t*t;
      return (float)Math.sqrt(dx2*t2 + dy2*t2);
      
    case QUADBEZIERTO:
      /* calculate the curve point at parameter value t */
      return quadBezierLength();
      
    case CUBICBEZIERTO:
      /* calculate the curve point at parameter value t */
      return cubicBezierLength();
    }
    
    return -1F;
  }
  
  /**
   * Use this to return arc length of a curve.  It returns the float representing the length given the value of the advancement parameter t on the curve. The current implementation of this function is very slow, not recommended for using during frame draw.
   * @eexample RCommand_getCurveLength
   * @return float, the length returned.
   * @invisible
   * */
  public float getCurveLength(){
    float arclength = 0F;
    
    switch(commandType){
    case LINETO:
      float dx = endPoint.x - startPoint.x;
      float dy = endPoint.y - startPoint.y;
      float dx2 = dx*dx;
      float dy2 = dy*dy;
      return (float)Math.sqrt(dx2 + dy2);
      
    case QUADBEZIERTO:
      /* calculate the curve point at parameter value t */
      return quadBezierLength();
      
    case CUBICBEZIERTO:
      /* calculate the curve point at parameter value t */
      return cubicBezierLength();
    }
    
    return -1F;
  }
  
  
  /**
   * Use this method to draw the command. 
   * @eexample drawCommand
   * @param g PGraphics, the graphics object on which to draw the command
   */
  public void draw(PGraphics g){
    RPoint[] points = getCurvePoints();
    if(points == null){
      return;
    }
    g.beginShape();
    for(int i=0;i<points.length;i++){
      g.vertex(points[i].x,points[i].y);
    }
    g.endShape();
  }
  
  
  /**
   * Use this method to get the bounding box of the command. 
   * @eexample getBounds
   * @return RContour, the bounding box of the command in the form of a fourpoint contour
   * @related draw ( )
   */
  public RContour getBounds(){
    float xmin =  Float.MAX_VALUE ;
    float ymin =  Float.MAX_VALUE ;
    float xmax = -Float.MAX_VALUE ;
    float ymax = -Float.MAX_VALUE ;
    
    RPoint[] points = this.getCurvePoints();
    if(points!=null){
      for( int i = 0 ; i < points.length ; i++ )
        {
          float x = points[i].x;
          float y = points[i].y;
          if( x < xmin ) xmin = x;
          if( x > xmax ) xmax = x;
          if( y < ymin ) ymin = y;
          if( y > ymax ) ymax = y;
        }
    }
    
    RContour c = new RContour();
    c.addPoint(xmin,ymin);
    c.addPoint(xmin,ymax);
    c.addPoint(xmax,ymax);
    c.addPoint(xmax,ymin);
    return c;
  }
  
  /**
   * Use this to return the start, control and end points of the curve.  It returns the points in the way of an array of RPoint.
   * @eexample getPoints
   * @return RPoint[], the vertices returned in an array.
   * */
  public RPoint[] getPoints(){
    RPoint[] result;
    if(controlPoints==null){
      result = new RPoint[2];
      result[0] = startPoint;
      result[1] = endPoint;
    }else{
      result = new RPoint[controlPoints.length+2];
      result[0] = startPoint;
      System.arraycopy(controlPoints,0,result,1,controlPoints.length);
      result[result.length-1] = endPoint;
    }
    return result;
  }
  
  private void quadBezierAdaptative(){
    addCurvePoint(new RPoint(startPoint));
    quadBezierAdaptativeRecursive(startPoint.x, startPoint.y, controlPoints[0].x, controlPoints[0].y, endPoint.x, endPoint.y, 0);
    addCurvePoint(new RPoint(endPoint));
  }
  
  
  
  private void quadBezierAdaptativeRecursive(float x1, float y1, float x2, float y2, float x3, float y3, int level){
    
    if(level > segmentRecursionLimit)
      {
        return;
      }
    
    // Calculate all the mid-points of the line segments
    //----------------------
    float x12   = (x1 + x2) / 2;
    float y12   = (y1 + y2) / 2;
    float x23   = (x2 + x3) / 2;
    float y23   = (y2 + y3) / 2;
    float x123  = (x12 + x23) / 2;
    float y123  = (y12 + y23) / 2;
    
    float dx = x3-x1;
    float dy = y3-y1;
    float d = Math.abs(((x2 - x3) * dy - (y2 - y3) * dx));
    
    if(d > segmentCollinearityEpsilon)
      { 
        // Regular care
        //-----------------
        if(d * d <= segmentDistTolSqr * (dx*dx + dy*dy))
          {
            // If the curvature doesn't exceed the distance_tolerance value
            // we tend to finish subdivisions.
            //----------------------
            if(segmentAngleTol < segmentAngleTolEpsilon)
              {
                addCurvePoint(new RPoint(x123, y123));
                return;
              }
            
            // Angle & Cusp Condition
            //----------------------
            float da = Math.abs((float)Math.atan2(y3 - y2, x3 - x2) - (float)Math.atan2(y2 - y1, x2 - x1));
            if(da >= Math.PI) da = 2*(float)Math.PI - da;
            
            if(da < segmentAngleTol)
              {
                // Finally we can stop the recursion
                //----------------------
                addCurvePoint(new RPoint(x123, y123));
                return;
              }
          }
      }
    else
      {
        if(Math.abs(x1 + x3 - x2 - x2) + Math.abs(y1 + y3 - y2 - y2) <= segmentDistTolMnhttn)
          {
            addCurvePoint(new RPoint(x123, y123));
            return;
          }
      }
    
    // Continue subdivision
    //----------------------
    quadBezierAdaptativeRecursive(x1, y1, x12, y12, x123, y123, level + 1);
    quadBezierAdaptativeRecursive(x123, y123, x23, y23, x3, y3, level + 1);
  }
  
  private void cubicBezierAdaptative(){
    addCurvePoint(new RPoint(startPoint));
    cubicBezierAdaptativeRecursive(startPoint.x, startPoint.y, controlPoints[0].x, controlPoints[0].y, controlPoints[1].x, controlPoints[1].y, endPoint.x, endPoint.y, 0);
    addCurvePoint(new RPoint(endPoint));
  }
  
  private void cubicBezierAdaptativeRecursive(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4, int level){
    if(level > segmentRecursionLimit)
      {
        return;
      }
    
    // Calculate all the mid-points of the line segments
    //----------------------
    float x12   = (x1 + x2) / 2;
    float y12   = (y1 + y2) / 2;
    float x23   = (x2 + x3) / 2;
    float y23   = (y2 + y3) / 2;
    float x34   = (x3 + x4) / 2;
    float y34   = (y3 + y4) / 2;
    float x123  = (x12 + x23) / 2;
    float y123  = (y12 + y23) / 2;
    float x234  = (x23 + x34) / 2;
    float y234  = (y23 + y34) / 2;
    float x1234 = (x123 + x234) / 2;
    float y1234 = (y123 + y234) / 2;
    
    // Try to approximate the full cubic curve by a single straight line
    //------------------
    float dx = x4-x1;
    float dy = y4-y1;
    
    float d2 = Math.abs(((x2 - x4) * dy - (y2 - y4) * dx));
    float d3 = Math.abs(((x3 - x4) * dy - (y3 - y4) * dx));
    float da1, da2;
    
    int d2b = (d2 > segmentCollinearityEpsilon)?1:0;
    int d3b = (d3 > segmentCollinearityEpsilon)?1:0;
    switch((d2b << 1) + d3b){
    case 0:
      // All collinear OR p1==p4
      //----------------------
      if(Math.abs(x1 + x3 - x2 - x2) +
         Math.abs(y1 + y3 - y2 - y2) +
         Math.abs(x2 + x4 - x3 - x3) +
         Math.abs(y2 + y4 - y3 - y3) <= segmentDistTolMnhttn)
        {
          addCurvePoint(new RPoint(x1234, y1234));
          return;
        }
      break;
      
    case 1:
      // p1,p2,p4 are collinear, p3 is considerable
      //----------------------
      if(d3 * d3 <= segmentDistTolSqr * (dx*dx + dy*dy))
        {
          if(segmentAngleTol < segmentAngleTolEpsilon)
            {
              addCurvePoint(new RPoint(x23, y23));
              return;
            }
          
          // Angle Condition
          //----------------------
          da1 = Math.abs((float)Math.atan2(y4 - y3, x4 - x3) - (float)Math.atan2(y3 - y2, x3 - x2));
          if(da1 >= (float)Math.PI) da1 = 2*(float)Math.PI - da1;
          
          if(da1 < segmentAngleTol)
            {
              addCurvePoint(new RPoint(x2, y2));
              addCurvePoint(new RPoint(x3, y3));
              return;
            }
          
          if(segmentCuspLimit != 0.0)
            {
              if(da1 > segmentCuspLimit)
                {
                  addCurvePoint(new RPoint(x3, y3));
                  return;
                }
            }
        }
      break;
      
    case 2:
      // p1,p3,p4 are collinear, p2 is considerable
      //----------------------
      if(d2 * d2 <= segmentDistTolSqr * (dx*dx + dy*dy))
        {
          if(segmentAngleTol < segmentAngleTolEpsilon)
            {
              addCurvePoint(new RPoint(x23, y23));
              return;
            }
          
          // Angle Condition
          //----------------------
          da1 = Math.abs((float)Math.atan2(y3 - y2, x3 - x2) - (float)Math.atan2(y2 - y1, x2 - x1));
          if(da1 >= (float)Math.PI) da1 = 2*(float)Math.PI - da1;
          
          if(da1 < segmentAngleTol)
            {
              addCurvePoint(new RPoint(x2, y2));
              addCurvePoint(new RPoint(x3, y3));
              return;
            }
          
          if(segmentCuspLimit != 0.0)
            {
              if(da1 > segmentCuspLimit)
                {
                  addCurvePoint(new RPoint(x2, y2));
                  return;
                }
            }
        }
      break;
      
    case 3: 
      // Regular care
      //-----------------
      if((d2 + d3)*(d2 + d3) <= segmentDistTolSqr * (dx*dx + dy*dy))
        {
          // If the curvature doesn't exceed the distance_tolerance value
          // we tend to finish subdivisions.
          //----------------------
          if(segmentAngleTol < segmentAngleTolEpsilon)
            {
              addCurvePoint(new RPoint(x23, y23));
              return;
            }
          
          // Angle & Cusp Condition
          //----------------------
          float a23 = (float)Math.atan2(y3 - y2, x3 - x2);
          da1 = Math.abs(a23 - (float)Math.atan2(y2 - y1, x2 - x1));
          da2 = Math.abs((float)Math.atan2(y4 - y3, x4 - x3) - a23);
          if(da1 >= (float)Math.PI) da1 = 2*(float)Math.PI - da1;
          if(da2 >= (float)Math.PI) da2 = 2*(float)Math.PI - da2;
          
          if(da1 + da2 < segmentAngleTol)
            {
              // Finally we can stop the recursion
              //----------------------
              addCurvePoint(new RPoint(x23, y23));
              return;
            }
          
          if(segmentCuspLimit != 0.0)
            {
              if(da1 > segmentCuspLimit)
                {
                  addCurvePoint(new RPoint(x2, y2));
                  return;
                }
              
              if(da2 > segmentCuspLimit)
                {
                  addCurvePoint(new RPoint(x3, y3));
                  return;
                }
            }
        }
      break;
    }
    
    // Continue subdivision
    //----------------------
    cubicBezierAdaptativeRecursive(x1, y1, x12, y12, x123, y123, x1234, y1234, level + 1);
    cubicBezierAdaptativeRecursive(x1234, y1234, x234, y234, x34, y34, x4, y4, level + 1);
  }
  
  private void lineUniformStep(){
    // If the number of steps is equal to 0 then choose a number of steps adapted to the curve
    int steps = segmentSteps;
    if(segmentSteps==0.0F){
      float dx = endPoint.x - startPoint.x;
      float dy = endPoint.y - startPoint.y;
      
      float len = (float)Math.sqrt(dx * dx + dy * dy);
      steps = (int)(len * 0.25);
      
      if(steps < 4) steps = 4;
    }
    
    float dt = 1F/steps;
    
    float fx, fy, fdx, fdy;
    float temp = dt * dt;
    
    fx = startPoint.x;
    fdx = (endPoint.x - startPoint.x) * dt;
    
    fy = startPoint.y;
    fdy = (endPoint.y - startPoint.y) * dt;
    
    for (int loop=0; loop < steps; loop++) {
      addCurvePoint(new RPoint(fx,fy));
      
      fx = fx + fdx;
      
      fy = fy + fdy;
    }
    addCurvePoint(new RPoint(endPoint));
  }
  
  private void cubicBezierUniformStep(){
    
    // If the number of steps is equal to 0 then choose a number of steps adapted to the curve
    int steps = segmentSteps;
    if(segmentSteps==0.0F){
      float dx1 = controlPoints[0].x - startPoint.x;
      float dy1 = controlPoints[0].y - startPoint.y;
      float dx2 = controlPoints[1].x - controlPoints[0].x;
      float dy2 = controlPoints[1].y - controlPoints[0].y;
      float dx3 = endPoint.x - controlPoints[1].x;
      float dy3 = endPoint.y - controlPoints[1].y;
      
      float len = (float)Math.sqrt(dx1 * dx1 + dy1 * dy1) +
        (float)Math.sqrt(dx2 * dx2 + dy2 * dy2) +
        (float)Math.sqrt(dx3 * dx3 + dy3 * dy3);
      
      steps = (int)(len * 0.25);
      
      if(steps < 4)
        {
          steps = 4;
        }
    }
    
    float dt = 1F/steps;
    
    float fx, fy, fdx, fdy, fddx, fddy, fdddx, fdddy, fdd_per_2x, fdd_per_2y, fddd_per_2x, fddd_per_2y, fddd_per_6x, fddd_per_6y;
    float temp = dt * dt;
    
    fx = startPoint.x;
    fdx = 3F * (controlPoints[0].x - startPoint.x) * dt;
    fdd_per_2x = 3F * (startPoint.x - 2F * controlPoints[0].x + controlPoints[1].x) * temp;
    fddd_per_2x = 3F * (3F * (controlPoints[0].x - controlPoints[1].x) + endPoint.x - startPoint.x) * temp * dt;
    
    fdddx = fddd_per_2x + fddd_per_2x;
    fddx = fdd_per_2x + fdd_per_2x;
    fddd_per_6x = fddd_per_2x * (1.0F / 3F);
    
    fy = startPoint.y;
    fdy = 3F * (controlPoints[0].y - startPoint.y) * dt;
    fdd_per_2y = 3F * (startPoint.y - 2F * controlPoints[0].y + controlPoints[1].y) * temp;
    fddd_per_2y = 3F * (3F * (controlPoints[0].y - controlPoints[1].y) + endPoint.y - startPoint.y) * temp * dt;
    
    fdddy = fddd_per_2y + fddd_per_2y;
    fddy = fdd_per_2y + fdd_per_2y;
    fddd_per_6y = fddd_per_2y * (1.0F / 3F);
    
    for (int loop=0; loop < steps; loop++) {
      addCurvePoint(new RPoint(fx,fy));
      
      fx = fx + fdx + fdd_per_2x + fddd_per_6x;
      fdx = fdx + fddx + fddd_per_2x;
      fddx = fddx + fdddx;
      fdd_per_2x = fdd_per_2x + fddd_per_2x;
      
      fy = fy + fdy + fdd_per_2y + fddd_per_6y;
      fdy = fdy + fddy + fddd_per_2y;
      fddy = fddy + fdddy;
      fdd_per_2y = fdd_per_2y + fddd_per_2y;
    }
    addCurvePoint(new RPoint(endPoint));
  }
  
  private void quadBezierUniformStep(){
    // If the number of steps is equal to 0 then choose a number of steps adapted to the curve
    int steps = segmentSteps;
    if(segmentSteps==0.0F){
      float dx1 = controlPoints[0].x - startPoint.x;
      float dy1 = controlPoints[0].y - startPoint.y;
      float dx2 = endPoint.x - controlPoints[0].x;
      float dy2 = endPoint.y - controlPoints[0].y;
      
      float len = (float)Math.sqrt(dx1 * dx1 + dy1 * dy1) + (float)Math.sqrt(dx2 * dx2 + dy2 * dy2);
      steps = (int)(len * 0.25);
      
      if(steps < 4) steps = 4;
    }
    
    float dt = 1F/steps;
    
    float fx, fy, fdx, fdy, fddx, fddy, fdd_per_2x, fdd_per_2y;
    float temp = dt * dt;
    
    fx = startPoint.x;
    fdx = 2F * (controlPoints[0].x - startPoint.x) * dt;
    fdd_per_2x = (startPoint.x - 2F * controlPoints[0].x + endPoint.x) * temp;
    fddx = fdd_per_2x + fdd_per_2x;
    
    fy = startPoint.y;
    fdy = 2F * (controlPoints[0].y - startPoint.y) * dt;
    fdd_per_2y = (startPoint.y - 2F * controlPoints[0].y + endPoint.y) * temp;
    fddy = fdd_per_2y + fdd_per_2y;
    
    for (int loop=0; loop < steps; loop++) {
      addCurvePoint(new RPoint(fx,fy));
      
      fx = fx + fdx + fdd_per_2x;
      fdx = fdx + fddx;
      
      fy = fy + fdy + fdd_per_2y;
      fdy = fdy + fddy;
    }
    addCurvePoint(new RPoint(endPoint));
  }
  
  // Use Horner's method to advance
  //----------------------
  private void lineUniformLength(){
    
    // If the number of steps is equal to 0 then choose a number of steps adapted to the curve
    float dx1 = endPoint.x - startPoint.x;
    float dy1 = endPoint.y - startPoint.y;
    
    float len = (float)Math.sqrt(dx1 * dx1 + dy1 * dy1);
    float steps = (int)(len * 2);
    
    if(steps < 4) steps = 4;
    
    // This holds the amount of steps used to calculate segment lengths
    float dt = 1F/steps;
    
    // This holds how much length has to bee advanced until adding a point
    float untilPoint = RCommand.segmentAccOffset;
    
    float fx, fy, fdx, fdy;
    
    fx = startPoint.x;
    fdx = (endPoint.x - startPoint.x) * dt;
    
    fy = startPoint.y;
    fdy = (endPoint.y - startPoint.y) * dt;
    
    for (int loop=0; loop <= steps; loop++) {
      /* Add point to curve if segment length is reached */
      if (untilPoint <= 0) {
        addCurvePoint(new RPoint(fx, fy));
        untilPoint += RCommand.segmentLength;
      }
      
      /* Add segment differential to segment length */
      untilPoint -= (float)Math.sqrt(fdx*fdx + fdy*fdy);    // Eventually try other distance measures
      
      fx = fx + fdx;
      fy = fy + fdy;
    }
    
    //addCurvePoint(new RPoint(endPoint));
    RCommand.segmentAccOffset = untilPoint;
  }
  
  // Use Horner's method to advance
  //----------------------
  private void quadBezierUniformLength(){
    
    float dx1 = controlPoints[0].x - startPoint.x;
    float dy1 = controlPoints[0].y - startPoint.y;
    float dx2 = endPoint.x - controlPoints[0].x;
    float dy2 = endPoint.y - controlPoints[0].y;
    float len = (float)Math.sqrt(dx1 * dx1 + dy1 * dy1) + (float)Math.sqrt(dx2 * dx2 + dy2 * dy2);
    float steps = (int)(len * 2);
    
    if(steps < 4) steps = 4;
    
    float dt = 1F/steps;
    float untilPoint = RCommand.segmentAccOffset;
    
    float fx, fy, fdx, fdy, fddx, fddy, fdd_per_2x, fdd_per_2y, fix, fiy;
    float temp = dt * dt;
    
    fx = startPoint.x;
    fdx = 2F * (controlPoints[0].x - startPoint.x) * dt;
    fdd_per_2x = (startPoint.x - 2F * controlPoints[0].x + endPoint.x) * temp;
    fddx = fdd_per_2x + fdd_per_2x;
    
    fy = startPoint.y;
    fdy = 2F * (controlPoints[0].y - startPoint.y) * dt;
    fdd_per_2y = (startPoint.y - 2F * controlPoints[0].y + endPoint.y) * temp;
    fddy = fdd_per_2y + fdd_per_2y;
    
    for (int loop=0; loop <= steps; loop++) {
      /* Add point to curve if segment length is reached */
      if (untilPoint <= 0) {
        addCurvePoint(new RPoint(fx, fy));
        untilPoint += RCommand.segmentLength;
      }
      
      /* Add segment differential to segment length */
      fix = fdx + fdd_per_2x;
      fiy = fdy + fdd_per_2y;
      untilPoint -= (float)Math.sqrt(fix*fix + fiy*fiy);    // Eventually try other distance measures
      
      fx = fx + fix;
      fdx = fdx + fddx;
      
      fy = fy + fiy;
      fdy = fdy + fddy;
    }
    
    //addCurvePoint(new RPoint(endPoint));
    RCommand.segmentAccOffset = untilPoint;
  }
  
  // Use Horner's method to advance
  //----------------------  
  private void cubicBezierUniformLength(){
    
    float dx1 = controlPoints[0].x - startPoint.x;
    float dy1 = controlPoints[0].y - startPoint.y;
    float dx2 = controlPoints[1].x - controlPoints[0].x;
    float dy2 = controlPoints[1].y - controlPoints[0].y;
    float dx3 = endPoint.x - controlPoints[1].x;
    float dy3 = endPoint.y - controlPoints[1].y;
    
    float len = (float)Math.sqrt(dx1 * dx1 + dy1 * dy1) +
      (float)Math.sqrt(dx2 * dx2 + dy2 * dy2) +
      (float)Math.sqrt(dx3 * dx3 + dy3 * dy3);
    float steps = (int)(len * 2);
    
    if(steps < 4) steps = 4;
    
    float dt = 1F/steps;
    float untilPoint = RCommand.segmentAccOffset;
    
    float fx, fy, fdx, fdy, fddx, fddy, fdddx, fdddy, fdd_per_2x, fdd_per_2y, fddd_per_2x, fddd_per_2y, fddd_per_6x, fddd_per_6y, fix, fiy;
    float temp = dt * dt;
    
    fx = startPoint.x;
    fdx = 3F * (controlPoints[0].x - startPoint.x) * dt;
    fdd_per_2x = 3F * (startPoint.x - 2F * controlPoints[0].x + controlPoints[1].x) * temp;
    fddd_per_2x = 3F * (3F * (controlPoints[0].x - controlPoints[1].x) + endPoint.x - startPoint.x) * temp * dt;
    fdddx = fddd_per_2x + fddd_per_2x;
    fddx = fdd_per_2x + fdd_per_2x;
    fddd_per_6x = fddd_per_2x * (1.0F / 3F);
    
    fy = startPoint.y;
    fdy = 3F * (controlPoints[0].y - startPoint.y) * dt;
    fdd_per_2y = 3F * (startPoint.y - 2F * controlPoints[0].y + controlPoints[1].y) * temp;
    fddd_per_2y = 3F * (3F * (controlPoints[0].y - controlPoints[1].y) + endPoint.y - startPoint.y) * temp * dt;
    
    fdddy = fddd_per_2y + fddd_per_2y;
    fddy = fdd_per_2y + fdd_per_2y;
    fddd_per_6y = fddd_per_2y * (1.0F / 3F);
    
    for (int loop=0; loop < steps; loop++) {
      /* Add point to curve if segment length is reached */
      if (untilPoint <= 0) {
        addCurvePoint(new RPoint(fx, fy));
        untilPoint += RCommand.segmentLength;
      }
      
      /* Add segment differential to segment length */
      fix = fdx + fdd_per_2x + fddd_per_6x;
      fiy = fdy + fdd_per_2y + fddd_per_6y;
      untilPoint -= (float)Math.sqrt(fix*fix + fiy*fiy);    // Eventually try other distance measures
      
      fx = fx + fix;
      fdx = fdx + fddx + fddd_per_2x;
      fddx = fddx + fdddx;
      fdd_per_2x = fdd_per_2x + fddd_per_2x;
      
      fy = fy + fiy;
      fdy = fdy + fddy + fddd_per_2y;
      fddy = fddy + fdddy;
      fdd_per_2y = fdd_per_2y + fddd_per_2y;
    }
    
    //addCurvePoint(new RPoint(endPoint));
    RCommand.segmentAccOffset = untilPoint;
  }
  
  private float quadBezierLength(){
    
    float dx1 = controlPoints[0].x - startPoint.x;
    float dy1 = controlPoints[0].y - startPoint.y;
    float dx2 = endPoint.x - controlPoints[0].x;
    float dy2 = endPoint.y - controlPoints[0].y;
    float len = (float)Math.sqrt(dx1 * dx1 + dy1 * dy1) + (float)Math.sqrt(dx2 * dx2 + dy2 * dy2);
    float steps = (int)(len * 2);
    
    if(steps < 4) steps = 4;
    
    float dt = 1F/steps;
    float untilPoint = RCommand.segmentAccOffset;
    
    float fx, fy, fdx, fdy, fddx, fddy, fdd_per_2x, fdd_per_2y, fix, fiy;
    float temp = dt * dt;
    float totallen = 0F;
    
    fx = startPoint.x;
    fdx = 2F * (controlPoints[0].x - startPoint.x) * dt;
    fdd_per_2x = (startPoint.x - 2F * controlPoints[0].x + endPoint.x) * temp;
    fddx = fdd_per_2x + fdd_per_2x;
    
    fy = startPoint.y;
    fdy = 2F * (controlPoints[0].y - startPoint.y) * dt;
    fdd_per_2y = (startPoint.y - 2F * controlPoints[0].y + endPoint.y) * temp;
    fddy = fdd_per_2y + fdd_per_2y;
    
    for (int loop=0; loop <= steps; loop++) {
      /* Add segment differential to segment length */
      fix = fdx + fdd_per_2x;
      fiy = fdy + fdd_per_2y;
      totallen += (float)Math.sqrt(fix*fix + fiy*fiy);    // Eventually try other distance measures
      
      fx = fx + fix;
      fdx = fdx + fddx;
      
      fy = fy + fiy;
      fdy = fdy + fddy;
    }
    
    return totallen;
  }
  
  
  private float cubicBezierLength(){
    
    float dx1 = controlPoints[0].x - startPoint.x;
    float dy1 = controlPoints[0].y - startPoint.y;
    float dx2 = controlPoints[1].x - controlPoints[0].x;
    float dy2 = controlPoints[1].y - controlPoints[0].y;
    float dx3 = endPoint.x - controlPoints[1].x;
    float dy3 = endPoint.y - controlPoints[1].y;
    
    float len = (float)Math.sqrt(dx1 * dx1 + dy1 * dy1) +
      (float)Math.sqrt(dx2 * dx2 + dy2 * dy2) +
      (float)Math.sqrt(dx3 * dx3 + dy3 * dy3);
    float steps = (int)(len * 2);
    
    if(steps < 4) steps = 4;
    
    float dt = 1F/steps;
    
    float fx, fy, fdx, fdy, fddx, fddy, fdddx, fdddy, fdd_per_2x, fdd_per_2y, fddd_per_2x, fddd_per_2y, fddd_per_6x, fddd_per_6y, fix, fiy;
    float temp = dt * dt;
    float totallen = 0F;
    
    fx = startPoint.x;
    fdx = 3F * (controlPoints[0].x - startPoint.x) * dt;
    fdd_per_2x = 3F * (startPoint.x - 2F * controlPoints[0].x + controlPoints[1].x) * temp;
    fddd_per_2x = 3F * (3F * (controlPoints[0].x - controlPoints[1].x) + endPoint.x - startPoint.x) * temp * dt;
    fdddx = fddd_per_2x + fddd_per_2x;
    fddx = fdd_per_2x + fdd_per_2x;
    fddd_per_6x = fddd_per_2x * (1.0F / 3F);
    
    fy = startPoint.y;
    fdy = 3F * (controlPoints[0].y - startPoint.y) * dt;
    fdd_per_2y = 3F * (startPoint.y - 2F * controlPoints[0].y + controlPoints[1].y) * temp;
    fddd_per_2y = 3F * (3F * (controlPoints[0].y - controlPoints[1].y) + endPoint.y - startPoint.y) * temp * dt;
    
    fdddy = fddd_per_2y + fddd_per_2y;
    fddy = fdd_per_2y + fdd_per_2y;
    fddd_per_6y = fddd_per_2y * (1.0F / 3F);
    
    for (int loop=0; loop < steps; loop++) {
      /* Add segment differential to segment length */
      fix = fdx + fdd_per_2x + fddd_per_6x;
      fiy = fdy + fdd_per_2y + fddd_per_6y;
      totallen += (float)Math.sqrt(fix*fix + fiy*fiy);    // Eventually try other distance measures
      
      fx = fx + fix;
      fdx = fdx + fddx + fddd_per_2x;
      fddx = fddx + fdddx;
      fdd_per_2x = fdd_per_2x + fddd_per_2x;
      
      fy = fy + fiy;
      fdy = fdy + fddy + fddd_per_2y;
      fddy = fddy + fdddy;
      fdd_per_2y = fdd_per_2y + fddd_per_2y;
    }
    
    return totallen;
  }
  
  
  /**
   * Use this method to transform the command. 
   * @eexample transformCommand
   * @param g PGraphics, the graphics object on which to apply an affine transformation to the command
   */
  /*
    public void transform(RMatrix m){
    int numControlPoints = countControlPoints();
    if(numControlPoints!=0){
    for(int i=0;i<numControlPoints;i++){
    controlPoints[i].transform(m);
    }
    }
    startPoint.transform(m);
    endPoint.transform(m);
    }
  */
  void append(RPoint nextcontrolpoint)
  {
    RPoint[] newcontrolPoints;
    if(controlPoints==null){
      newcontrolPoints = new RPoint[1];
      newcontrolPoints[0] = nextcontrolpoint;
    }else{
      newcontrolPoints = new RPoint[controlPoints.length+1];
      System.arraycopy(controlPoints,0,newcontrolPoints,0,controlPoints.length);
      newcontrolPoints[controlPoints.length]=nextcontrolpoint;
    }
    this.controlPoints=newcontrolPoints;
  }
  
  private void addCurvePoint(RPoint nextcurvepoint)
  {
    RPoint[] newcurvePoints;
    if(curvePoints==null){
      newcurvePoints = new RPoint[1];
      newcurvePoints[0] = nextcurvepoint;
    }else{
      newcurvePoints = new RPoint[curvePoints.length+1];
      System.arraycopy(curvePoints,0,newcurvePoints,0,curvePoints.length);
      newcurvePoints[curvePoints.length]=nextcurvepoint;
    }
    this.curvePoints=newcurvePoints;
  }
}