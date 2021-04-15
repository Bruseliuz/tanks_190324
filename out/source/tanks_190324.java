import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import ddf.minim.*; 
import java.lang.reflect.InvocationTargetException; 
import java.lang.reflect.Method; 
import java.util.HashSet; 
import java.util.Set; 
import java.util.HashSet; 
import java.util.Set; 
import java.util.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class tanks_190324 extends PApplet {

/*
* JUST NU: 

*/

// för ljud




// Ljud
Minim minim;
SoundManager soundManager;

boolean debugOn = false;
boolean pause = false;
boolean gameOver = false;

Grid grid;
int cols = 15;
int rows = 15;
int grid_size = 50;

// Boolean variables connected to keys
boolean left, right, up, down;
boolean fire;
boolean alt_key; // Turning turret, alt+LEFT/RIGHT

boolean mouse_pressed;
boolean userControl;
int tankInFocus;

int team0Color = color(204, 50, 50);
int team1Color = color(0, 150, 200);

CannonBall[] allShots = new CannonBall[6];

Tree[] allTrees = new Tree[3];

Tank[] allTanks = new Tank[6];

Team[] teams = new Team[2];

int tank_size = 50;
// Team0
PVector team0_tank0_startpos;
PVector team0_tank1_startpos;
PVector team0_tank2_startpos;

// Team1
PVector team1_tank0_startpos;
PVector team1_tank1_startpos;
PVector team1_tank2_startpos;

// timer
int savedTime;
int wait = 3000; //wait 3 sec (reload)
boolean tick;

Timer timer;
int startTime = 1; //minutes 
int remainingTime;

public void setup(){
  
  
  soundManager = new SoundManager(this);
  soundManager.addSound("tank_firing");
  soundManager.addSound("tank_idle");
  soundManager.addSound("blast");
  
  grid = new Grid(cols, rows, grid_size);
  
  //grid = new Node[cols][rows];
  //for (int i = 0; i < cols; i++) {
  //  for (int j = 0; j < rows; j++) {
  //    // Initialize each object
  //    grid[i][j] = new Node(i,j,i*grid_size, j*grid_size);
  //  }
  //}
  
  
  // Skapa alla träd
  allTrees[0] = new Tree(230, 600);
  allTrees[1] = new Tree(280,230);//280,230(300,300)
  allTrees[2] = new Tree(530, 520);//530, 520(500,500);

  // Skapa alla skott
  for (int i = 0; i < allShots.length; i++) {
    allShots[i] = new CannonBall();
  }
  
  // Team0
  team0_tank0_startpos = new PVector(50, 50);
  team0_tank1_startpos = new PVector(50, 150);
  team0_tank2_startpos = new PVector(50, 250);

  // Team1
  team1_tank0_startpos = new PVector(width-50, height-250);
  team1_tank1_startpos = new PVector(width-50, height-150);
  team1_tank2_startpos = new PVector(width-50, height-50);
  
 
  // nytt Team: id, color, tank0pos, id, shot
  teams[0] = new Team1(0, tank_size, team0Color, 
                      team0_tank0_startpos, 0, allShots[0],
                      team0_tank1_startpos, 1, allShots[1],
                      team0_tank2_startpos, 2, allShots[2]);
  
  allTanks[0] = teams[0].tanks[0];
  allTanks[1] = teams[0].tanks[1];
  allTanks[2] = teams[0].tanks[2];
  
  teams[1] = new Team2(1, tank_size, team1Color, 
                      team1_tank0_startpos, 3, allShots[3],
                      team1_tank1_startpos, 4, allShots[4],
                      team1_tank2_startpos, 5, allShots[5]);
  
  allTanks[3] = teams[1].tanks[0];
  allTanks[4] = teams[1].tanks[1];
  allTanks[5] = teams[1].tanks[2];
  
  loadShots();
  userControl = false;
  tankInFocus = 0;
  
  savedTime = millis(); //store the current time.
  
  remainingTime = startTime;
  timer = new Timer();
  timer.setDirection("down");
  timer.setTime(startTime);
}


public void draw() {
  background(200);
  checkForInput(); // Kontrollera inmatning.
  
  if (!gameOver && !pause) {
    // timer används inte i dagsläget.
    timer.tick(); // Alt.1
    float deltaTime = timer.getDeltaSec();
    remainingTime = PApplet.parseInt(timer.getTotalTime()); 
    if (remainingTime <= 0) {
      remainingTime = 0;
      timer.pause();
      gameOver = true;  
    }
    
    int passedTime = millis() - savedTime; // Alt.2
    
    //check the difference between now and the previously stored time is greater than the wait interval
    if (passedTime > wait){    
      //savedTime = millis();//also update the stored time   
    }
    
    // UPDATE LOGIC
    updateTanksLogic();
    updateTeamsLogic();
    
    // UPDATE TANKS
    updateTanks();
    updateShots();
   
    // CHECK FOR COLLISIONS
    checkForCollisionsShots(); 
    checkForCollisionsTanks();  
    
  }
  
  // UPDATE DISPLAY  
   teams[0].displayHomeBase();
   teams[1].displayHomeBase();
   displayTrees();
   updateTanksDisplay();  
   updateShotsDisplay();
   
   
  
  showGUI();
  
}

// Används inte
public float getTime() {
  return 0; //Dummy temp
}
class CannonBall extends Sprite { 
  // Explosion
  ArrayList<Particle> particles;
  
  PVector positionPrev; //spara temp senaste pos.
  // All of our regular motion stuff
  //PVector position;
  PVector velocity;
  PVector acceleration;
  
  //String name;
  
  //PVector hitArea;
  //float diameter, radius;

  boolean isLoaded;  // The shot is loaded and ready to shoot (visible on screen.)
  boolean isInMotion; // The shot is on its way.
  boolean isExploding;
  boolean isVisible; 
  
  //Cannonball måste kunna skicka info om träff till sin ägare(den som skjutit (poäng).)
  Tank owner;

  // Size
  float r = 8;
  
  int my_color;

  float topspeed = 10;
  int savedTime;
  int passedTime;
  boolean isCounting;

  //**************************************************
  CannonBall() {
    //println("New CannonBall()");

    this.positionPrev = new PVector();
    this.position = new PVector();
    this.velocity = new PVector();
    this.acceleration = new PVector();
    this.isInMotion = false;
    this.isCounting = false;
    this.isVisible = true;
    
    //this.diameter = this.img.width/2;
    this.radius = this.r;
    this.isExploding = false;
    
    this.name = "bullet";
  }

  //**************************************************  
  public void setColor(int c) {
    this.my_color = c;
  }
  
  //**************************************************  
  public void setOwner(Tank t) {
    this.owner = t;
  }

  //**************************************************  
  //String getName(){
  //  return this.name;
  //}
  
  //**************************************************  
  public PVector position() {
    return this.position;  
  }

  //**************************************************
  // Called by tank object.
  public void updateLoadedPosition(PVector pvec) {
    //println("*** CannonBall.updateLoadedPosition()");

    this.position.set(pvec);
    this.positionPrev.set(this.position);
    if (!this.isVisible) {
      this.isVisible = true;
    }
  }

  //**************************************************
  // Called by tank object.
  public void loaded() {
    println("*** CannonBall.loaded()");
    this.isLoaded = true;
  }

  //**************************************************
  // Called by tank object, when shooting.
  public void applyForce(PVector force) {
    this.acceleration.add(force);
  }

  //**************************************************  
  public void startTimer() {
    //println("*** CannonBall.startTimer()");
    this.isCounting = true;
    this.savedTime = millis();
    this.passedTime = 0;
    
  }
  
  //**************************************************  
  public void resetTimer() {
    //println("*** CannonBall.resetTimer()");
    this.isCounting = false;
    this.savedTime = 0;
    this.passedTime = 0;
    
    this.isInMotion = false;
    this.isLoaded = false;
        
    this.velocity.set(0,0,0);
    this.acceleration.set(0,0,0);
  }

  //**************************************************  
  public void displayExplosion() {
    this.isExploding = true;
    this.isInMotion = false;
    this.isVisible = false;
    
    
    this.particles = new ArrayList<Particle>();
    this.particles.add(new Particle(new PVector(0,0)));
    this.particles.add(new Particle(new PVector(0,0)));
    this.particles.add(new Particle(new PVector(0,0)));
  }

  //**************************************************  
  public void update() { 
    if (this.isInMotion) {
      this.positionPrev.set(this.position); // spara senaste pos.
      
      this.velocity.add(this.acceleration);
      this.velocity.limit(this.topspeed);
      this.position.add(this.velocity);
      this.acceleration.mult(0);
      
    }
    
    if (isCounting) {
      this.passedTime = millis() - this.savedTime;
    }
  }

  //**************************************************  
  public void checkCollision() {
    if (this.isInMotion) {
    }
  }
  
  //**************************************************
  public void checkCollision(Tree other) {
     if (this.isInMotion) {
        //println("*** Tank.checkCollision(Tree)");
    
        // Get distances between the balls components
        PVector distanceVect = PVector.sub(other.position, this.position);
    
        // Calculate magnitude of the vector separating the balls
        float distanceVectMag = distanceVect.mag();
    
        // Minimum distance before they are touching
        float minDistance = this.radius + other.radius;
    
        if (distanceVectMag <= minDistance) {
          println("collision Tree");
          this.position.set(this.positionPrev); // Flytta tillbaka.
          //this.acceleration.set(0,0,0);
          //this.velocity.set(0,0,0);
          
          displayExplosion();
          
        }
      }
    }
  
  //**************************************************  
  public void checkCollision(Tank other) {
    if (this.isInMotion) {
      //println("*** CannonBall.checkCollision(Tank): " + other);
  
      // Get distances between the balls components
      PVector distanceVect = PVector.sub(other.position, this.position);
  
      // Calculate magnitude of the vector separating the balls
      float distanceVectMag = distanceVect.mag();
  
      // Minimum distance before they are touching
      float minDistance = this.radius + other.radius;
  
      if (distanceVectMag < minDistance) {
        println("CannonBall collision Tank");
        this.position.set(this.positionPrev); // Flytta tillbaka.
        //this.acceleration.set(0,0,0);
        //this.velocity.set(0,0,0);
        //explosion(this.position);
        boolean succHit = other.takeDamage();
        if (succHit) {
          this.owner.successfulHit();
        }
        
        displayExplosion();
        
      }
    }
  }

  //**************************************************
  public void display() { 
      imageMode(CENTER);
      stroke(0);
      strokeWeight(2);
      fill(this.my_color);
      
      pushMatrix();
      translate(this.position.x,this.position.y);
      
      if (this.isExploding) {
        for(int i=0; i < this.particles.size(); i++){
          this.particles.get(i).run();
          if (this.particles.get(i).isDead()) {
            this.isExploding = false;
          }
        }
      }
      else {
        if (!(this.isCounting && !this.isInMotion) && this.isVisible) {
          ellipse(0, 0, this.r*2, this.r*2);
        }
      }
      popMatrix();
      
      // Nedan är bara en nödlösning för att återställa vissa varabler som ändrats i particles.
      fill(this.my_color);
      stroke(0);
      strokeWeight(1);
  }
}
/*
Denna FSM används inte.

     _  _(o)_(o)_  _
   ._\`:_ F S M _:' \_,
       / (`---'\ `-.
    ,-`  _)    (_, 

(not the Flying Spaghetti Monster, but ...)

A simple Finite State Machine library for Processing!

Based on the AlphaBeta FSM library for Arduino: http://www.arduino.cc/playground/Code/FiniteStateMachine
(Matches that API as closely as possible (with the exception of using string names for functions instead
of function pointers since function pointers are not available in Java).)

Learn more about Finite State Machines: http://en.wikipedia.org/wiki/Finite-state_machine

Usage:

Declare one FSM object and as many State objects as you like.

To initialize a State you need to pass in three strings representing the names of three functions
you've implemented in your sketch. These functions will be called when the state goes through its transitions: 

State playMode = new State("enterPlayMode", "doPlayMode", "exitPlayMode");

The first function will be called once each time the FSM enters this state. (enter function)
The second function will be called repeated as long as the FSM stays in this state. (execute function)
The third function will be called one when the FSM transition away from this state. (exit function)

When you initialize the FSM object, you pass it the state you'd like it to begin in:

FSM game;

game = new FSM(startMode);

In draw(), call the game's update() function. This will ensure that the library calls the appropriate state's execute function.

To transition to a different state, call:

game.transitionTo(someState);

To retrieve the state the FSM is currently in, call:

game.getCurrentState();

To test if the game is in a current state, call:

if(game.isInState(someState){
 // do something
}


*/




class FSM {
  State currentState;

  FSM(State initialState) {
    currentState = initialState;
  }

  public void update() {
    currentState.executeFunction();
  }
  
  public State getCurrentState(){
    return currentState;
  }
  
  public boolean isInState(State state){
   return currentState == state;
  }

  public void transitionTo(State newState) {
    currentState.exitFunction();
    currentState = newState;
    currentState.enterFunction();
  }
}

class State {
  PApplet parent;
  Method enterFunction;
  Method executeFunction;
  Method exitFunction;

  State(PApplet p, String enterFunctionName, String executeFunctionName, String exitFunctionName) {
    parent = p;

    Class sketchClass = parent.getClass();
    try { 

      enterFunction = sketchClass.getMethod(enterFunctionName);
      executeFunction = sketchClass.getMethod(executeFunctionName);
      exitFunction = sketchClass.getMethod(exitFunctionName);
    }

    catch(NoSuchMethodException e) {
      println("One of the state transition function is missing.");
    }
  }

  public void enterFunction() {
    try {
      enterFunction.invoke(parent);
    } 

    catch(IllegalAccessException e) {
      println("State enter function is missing or something is wrong with it.");
    }
    catch(InvocationTargetException e) {
      println("State enter function is missing.");
    }
  }
  public void executeFunction() {
    try {
      executeFunction.invoke(parent);
    }
    catch(IllegalAccessException e) {
      println("State execute function is missing or something is wrong with it.");
    }
    catch(InvocationTargetException e) {
      println("State execute function is missing.");
    }
    
  }
  public void exitFunction() {
    try {
      exitFunction.invoke(parent);
    }
    catch(IllegalAccessException e) {
      println("State exit function is missing or something is wrong with it.");
    }
    catch(InvocationTargetException e) {
      println("State exit function is missing.");
    }
  }
}
class Grid {
  int cols, rows;
  int grid_size;
  Node[][] nodes;

  //***************************************************  
  Grid(int _cols, int _rows, int _grid_size) {
    cols = _cols;
    rows = _rows;
    grid_size = _grid_size;
    nodes = new Node[cols][rows];

    createGrgetId();
  }
  public int getCols(){
    return cols;
  }
  public int getRows(){
    return rows;
  }

  //***************************************************  
  public void createGrgetId() {

    for (int i = 0; i < cols; i++) {
      for (int j = 0; j < rows; j++) {
        // Initialize each object
        nodes[i][j] = new Node(i, j, i*grid_size+grid_size, j*grid_size+grid_size);
      }
    }
  }

  //***************************************************  
  // ANVÄNDS INTE!
  public void display1() {
    for (int i = 0; i < cols; i++) {
      for (int j = 0; j < rows; j++) {
        // Initialize each object
        line(j*grid_size+grid_size, 0, j*grid_size+grid_size, height);
      }
      line(0, i*grid_size+grid_size, width, i*grid_size+grid_size);
    }
  }

  //***************************************************  
  public void display() {
    for (int i = 0; i < cols; i++) {
      for (int j = 0; j < rows; j++) {
        // Initialize each object
        ellipse(nodes[i][j].position.x, nodes[i][j].position.y, 5.0f, 5.0f);
        //println("nodes[i][j].position.x: " + nodes[i][j].position.x);
        //println(nodes[i][j]);
      }
      //line(0, i*grid_size+grid_size, width, i*grid_size+grid_size);
    }
  }

  //***************************************************  
  // ANVÄNDS INTE!
  public PVector getNearestNode1(PVector pvec) {
    //PVector pvec = new PVector(x,y);
    PVector vec = new PVector(0, 0);
    for (int i = 0; i < cols; i++) {
      for (int j = 0; j < rows; j++) {
        if (nodes[i][j].position.dist(pvec) < grid_size/2) {
          vec.set(nodes[i][j].position);
        }
      }
    }
    return vec;
  }

  public Node getNode(int x, int y) {
    return nodes[x][y];
  }

  //***************************************************  
  public Node getNearestNode(PVector pvec) {
    // En justering för extremvärden.
    float tempx = pvec.x;
    float tempy = pvec.y;
    if (pvec.x < 5) { 
      tempx=5;
    } else if (pvec.x > width-5) {
      tempx=width-5;
    }
    if (pvec.y < 5) { 
      tempy=5;
    } else if (pvec.y > height-5) {
      tempy=height-5;
    }

    pvec = new PVector(tempx, tempy);


    ArrayList<Node> nearestNodes = new ArrayList<Node>();
    for (int i = 0; i < cols; i++) {
      for (int j = 0; j < rows; j++) {
        if (nodes[i][j].position.dist(pvec) < grid_size) {
          nearestNodes.add(nodes[i][j]);
        }
      }
    }

    Node nearestNode = new Node(0, 0);
    for (int i = 0; i < nearestNodes.size(); i++) {
      if (nearestNodes.get(i).position.dist(pvec) < nearestNode.position.dist(pvec) ) {
        nearestNode = nearestNodes.get(i);
      }
    }

    return nearestNode;
  }

  // Node getNearestNodePosition(PVector pvec) {

  //  ArrayList<Node> nearestNodes = new ArrayList<Node>();

  //  for (int i = 0; i < cols; i++) {
  //    for (int j = 0; j < rows; j++) {
  //      if (nodes[i][j].position.dist(pvec) < grid_size) {
  //        nearestNodes.add(nodes[i][j]);      
  //      }
  //    }
  //  }

  //  Node nearestNode = new Node(0,0);
  //  for (int i = 0; i < nearestNodes.size(); i++) {
  //    if (nearestNodes.get(i).position.dist(pvec) < nearestNode.position.dist(pvec) ) {
  //      nearestNode = nearestNodes.get(i);
  //    }
  //  }

  //  return nearestNode;
  //}
  
  //***************************************************  
  public PVector getNearestNodePosition(PVector pvec) {
    Node n = getNearestNode(pvec);
    
    return n.position;
  }

  //***************************************************  
  // ANVÄNDS INTE?
  public void displayNearestNode(float x, float y) {
    PVector pvec = new PVector(x, y);
    displayNearestNode(pvec);
  }

  //***************************************************  
  // ANVÄNDS INTE!
  public void displayNearestNode1(PVector pvec) {
    //PVector pvec = new PVector(x,y);
    for (int i = 0; i < cols; i++) {
      for (int j = 0; j < rows; j++) {
        if (nodes[i][j].position.dist(pvec) < grid_size/2) {
          PVector vec = nodes[i][j].position;
          ellipse(vec.x, vec.y, 5, 5);
        }
      }
    }
  }

  //***************************************************  
  public void displayNearestNode(PVector pvec) {

    PVector vec = getNearestNodePosition(pvec);
    ellipse(vec.x, vec.y, 5, 5);
  }

  //***************************************************  
  public PVector getRandomNodePosition() {
    int c = PApplet.parseInt(random(cols));
    int r = PApplet.parseInt(random(rows));

    PVector rn = nodes[c][r].position;

    return rn;
  }


  public ArrayList<Node> getNearestNodes(Node node){
    ArrayList<Node> nearestNodes = new ArrayList<Node>();
    int col = node.getCol();
    int row = node.getRow();


    //Tar ovanför
    if(row != 0){
      if(node.isEmpty){
         nearestNodes.add(nodes[col][row-1]);
      }
    }
    //tar neråt
    if(row != rows-1){
      if(node.isEmpty){
      nearestNodes.add(nodes[col][row+1]);
      }
    }
    //Tar till vänster
    if(col != 0){
      if(node.isEmpty){
      nearestNodes.add(nodes[col-1][row]);
      }
    }
    //tar höger
    if(col != cols-1){
      if(node.isEmpty){
      nearestNodes.add(nodes[col+1][row]);
      }
    }
    return nearestNodes;
  }


  
  //***************************************************
  // Används troligen tillsammans med getNearestNode().empty
  // om tom så addContent(Sprite)
  public void addContent(Sprite s) {
    Node n = getNearestNode(s.position);
    n.addContent(s);
  }
  
}
class Node {
  // A node object knows about its location in the grid 
  // as well as its size with the variables x,y,w,h
  float x,y;   // x,y location
  float w,h;   // width and height
  float angle; // angle for oscillating brightness
  
  PVector position;
  int col, row;
  
  Sprite content;
  boolean isEmpty;
  
  //***************************************************
  // Node Constructor 
  // Denna används för temporära jämförelser mellan Node objekt.
  Node(float _posx, float _posy) {
    this.position = new PVector(_posx, _posy);
  }

  //***************************************************  
  // Används vid skapande av grid
  Node(int _id_col, int _id_row, float _posx, float _posy) {
    this.position = new PVector(_posx, _posy);
    this.col = _id_col;
    this.row = _id_row;
    
    this.content = null;
    this.isEmpty = true;
  } 

  //***************************************************  
  Node(float tempX, float tempY, float tempW, float tempH, float tempAngle) {
    x = tempX;
    y = tempY;
    w = tempW;
    h = tempH;
    angle = tempAngle;
  } 

  //***************************************************  
  public void addContent(Sprite s) {
    if (this.isEmpty) {
      this.content = s;  
    }
  }

  public int getCol(){
    return col;
  }
  public int getRow(){
    return row;
  }

  //***************************************************
  public boolean empty() {
    return this.isEmpty;
  }

  //***************************************************
  public Sprite content() {
    return this.content;
  }
}
class Particle {
  PVector position;
  PVector velocity;
  PVector acceleration;
  float lifespan;

  Particle(PVector l) {
    this.acceleration = new PVector(0, 1);
    this.position = new PVector().set(l);
    this.velocity = new PVector(random(-1, 1), random(-1, 1));
    //position = l.get();
    
    lifespan = 255.0f;
  }

  public void run() {
    update();
    display();
  }

  // Method to update position
  public void update() {
    //velocity.add(acceleration);
    position.add(velocity);
    lifespan -= 7.0f;
  }

  // Method to display
  public void display() {
    //println("lifespan: " + lifespan);
    stroke(0, lifespan);
    strokeWeight(2);
    fill(127, lifespan);
    ellipse(position.x, position.y, 100-lifespan, 100-lifespan);
  }

  // Is the particle still useful?
  public boolean isDead() {
    if (lifespan < 0.0f) {
      return true;
    } 
    else {
      return false;
    }
  }
}
class SensorReading {
  float distance;
  float heading;
  Sprite obj;
  
  SensorReading() {
    obj = null;
    distance = 0F;
    heading = 0F;
  }
  
  SensorReading(Sprite _obj, float _distance, float _heading) {
    obj = _obj;
    distance = _distance;
    heading = _heading;
  }
  
  public Sprite obj() {
    return obj; 
  }
  
  public float getHeading() {
   return heading; 
  }
  
  public float distance() {
   return distance; 
  }
  
  
  
  
}

// Används ännu inte.
class Sensor{

  protected Tank tank;
  boolean disabled;

  Sensor(Tank t){
    this.tank = t;
    this.disabled = false;
  }

  protected Tank getTank(){
    return tank;
  }
  
  public boolean disabled(){
    return this.disabled;
  }

  /*
    Performs readings
  */
  public float[] readValues(){
    return null;
  }

  /*
    Performs readings and returns the value at index
  */
 //  public PVector readValue(){
 //   return new PVector();
 //}
  
 //public float readValue1(){
 //   return 0.0;
 //}
 
  public SensorReading readValue(){
    return new SensorReading();
  }
  
  public float readValue(int index){
    return readValues()[index];
  }

  /**
   * Applies random noise to a given value
   *
   * @return The new computed reading
   */
  protected float getReadingAfterNoise(float reading, float noise) {
    float addedNoise = (float) Math.random() * noise - noise / 2f;
    return reading + addedNoise;
  }
}
// Används ännu inte.
//import processing.core.*;

class SensorBall extends Sensor{

  float[] values = new float[2];
  float sensorLimit = 1f;

  SensorBall(Tank t){
    super(t);
  }

  float lastRead = 0;
  //public float[] readValues(){
  //  // Avoid multiple readings within 100ms
  //  if(game.getTime() >= lastRead + 0.1f)
  //    doReading();

  //  return values;
  //}
  public float[] readValues(){
    // Avoid multiple readings within 100ms
    if(getTime() >= lastRead + 0.1f)
      doReading();

    return values;
  }

  private void doReading(){
    Tank thisTank = getTank();

    
    for (int i = 0; i < allShots.length; i++) {
      CannonBall ball = allShots[i];
    
      // Check if ball is turned off
      if(!ball.isVisible){
        values[0] = 0;
        values[1] = 0;
        return;
      }
  
      // Find relative distance from Ball to Tank
      PVector dist = PVector.sub(ball.position, thisTank.position);
      dist.rotate(-thisTank.getHeading());
  
      // index 0 contains the Angle of the ball
      values[0] = (float)Math.toDegrees(dist.heading());
      // index 1 contains the distance to the ball
      values[1] = (float)Math.min(dist.mag(), sensorLimit);
    }
  }

}
//Används ännu inte
//import processing.core.*;

class SensorCompass extends Sensor{

  // Random noise applyed to the final reading
  private static final int NOISE_AMOUNT = 3;

  // Interval to read the sensor in seconds
  private static final float READ_INTERVAL = 0.01f;

  float[] values = new float[1];

  //SensorCompass(GameSimulator g, Tank t){
  //  super(g, t);
  //}
  SensorCompass(Tank t){
    super(t);
  }

  float lastRead = 0;
  public float[] readValues(){
    //if(game.getTime() >= lastRead + READ_INTERVAL)
    if(getTime() >= lastRead + READ_INTERVAL)
      doReading();

    return values;
  }

  private void doReading(){
    Tank thisTank = getTank();

    //float orientation = (float)Math.toDegrees(thisTank.orientation);
    //float orientation = (float)Math.toDegrees(thisTank.heading);
    float orientation = (float) thisTank.getHeadingInDegrees(); Math.toDegrees(thisTank.heading);

    // Fix orientation (from 0-359)
    int multiples = (int)(orientation / 360);
    orientation = orientation - multiples * 360;
    //orientation = MathUtil.fixAngle(orientation);
    orientation = fixAngle(orientation);

    values[0] = getReadingAfterNoise(orientation, NOISE_AMOUNT);
  }

}
// Används ännu inte.
//import processing.core.*;

/**
 * Simulates an ultrasonic distance sensor.
 * Readings are synchronous and blocking.
 */

class SensorDistance extends Sensor{

  // Random noise applied to the final reading
  private static final float NOISE_AMOUNT = .01f;

  // Angle in degrees between tank's heading and sensor heading
  public float localOrientation = 0f;

  //SensorDistance(GameSimulator g, Tank r, float localOrientation) {
  //  super(g, r);
  //  this.localOrientation = localOrientation;
  //}
  SensorDistance(Tank t, float localOrientation) {
    super(t);
    this.localOrientation = localOrientation;
  }
  
  public boolean canSee(PVector obj) {
    /*
     PVector toTarget = new PVector(obj
     
     //--------
     // Check for collisions with Canvas Boundaries
  for (int i = 0; i < allTanks.length; i++) {
    allTanks[i].checkEnvironment();
    
    // Check for collisions with "no Smart Objects", Obstacles (trees, etc.)
    for (int j = 0; j < allTrees.length; j++) {
      allTanks[i].checkCollision(allTrees[j]);
    }
    
    // Check for collisions with "Smart Objects", other Tanks.
    for (int j = 0; j < allTanks.length; j++) {
      //if ((allTanks[i].getId() != j) && (allTanks[i].health > 0)) {
      if (allTanks[i].getId() != j) {
        allTanks[i].checkCollision(allTanks[j]);
      }
    }
  }
     
     
     
     
     //-------
      PVector force = PVector.fromAngle(this.heading);
        force.mult(10);
        this.ball.applyForce(force);
        
        
     //-------
      // Kontroll om att tanken inte "fastnat" i en annan tank. 
          distanceVect = PVector.sub(other.position, this.position);
          distanceVectMag = distanceVect.mag();
          if (distanceVectMag < minDistance) {
            println("FAST I ETT TRÄD");  
          }
        
    */
    
    return false;
  }
  
   //******************************************************
  // Returnera koordinaten som PVector, där tankens heading korsar kanterna på fönstret.
  public PVector readValue1(){
    
    PVector v11, v12, v21, v22, h11, h12, h21, h22;
    PVector t = new PVector(); 
    t = tank.position.copy();
    float angle = tank.heading; // - PI/2;
    PVector force = new PVector(cos(angle),sin(angle));
    force.mult(1000);
    t.add(force);
    
    v11 = new PVector(0, 0);          // vänster kant
    v12 = new PVector(0, height);
    h11 = new PVector(0, 0);          // övre kant
    h12 = new PVector(width, 0);
    v21 = new PVector(width, 0);      // höger kant
    v22 = new PVector(width, height);
    h21 = new PVector(0 , height);    // nerdre kant
    h22 = new PVector(width, height);
   
    
    // Returnera koordinaten som PVector, där tankens heading korsar kanterna på fönstret.
    PVector pvec = new PVector();
    
    pvec = line_line_p(tank.position, t, v21, v22); // höger kant.
    if (pvec == null) {
      pvec = line_line_p(tank.position, t, v11, v12); // vänster kant.
    }
    if (pvec == null) {
      pvec = line_line_p(tank.position, t, h11, h12); // övre kant.
    }
    if (pvec == null) {
      pvec = line_line_p(tank.position, t, h21, h22); // nedre kant.
    }
    
    return pvec;
  }

  
  //******************************************************
  // Returnera avståndet till kanten i tankens riktning. //<>//
  public SensorReading readValue(){
    
    PVector v11, v12, v21, v22, h11, h12, h21, h22;
    PVector tpos_cp = new PVector(); 
    tpos_cp = tank.position.copy();
    float angle = tank.heading; // - PI/2;
    PVector tpos = tank.position;
    PVector force = new PVector(cos(angle),sin(angle));
    force.mult(1000);
    tpos_cp.add(force);
    
    v11 = new PVector(0, 0);          // vänster kant
    v12 = new PVector(0, height);
    h11 = new PVector(0, 0);          // övre kant
    h12 = new PVector(width, 0);
    v21 = new PVector(width, 0);      // höger kant
    v22 = new PVector(width, height);
    h21 = new PVector(0 , height);    // nedre kant
    h22 = new PVector(width, height);
   
    
    // Returnera koordinaten som PVector, där tankens heading korsar kanterna på fönstret.
    PVector pvec = new PVector();
    
    pvec = line_line_p(tpos, tpos_cp, v21, v22); // höger kant.
    
    if (pvec == null) {
      pvec = line_line_p(tpos, tpos_cp, v11, v12); // vänster kant.
    }
    if (pvec == null) {
      pvec = line_line_p(tpos, tpos_cp, h11, h12); // övre kant.
    }
    if (pvec == null) {
      pvec = line_line_p(tpos, tpos_cp, h21, h22); // nedre kant.
    }
    
    // Om pvec fortfarande är null, så ge den ett värde.
    if (pvec == null) {
      pvec = tpos;  
    }
    
    // Get distances between the tanks components
    PVector distanceVect = PVector.sub(pvec, tpos);

    // Calculate magnitude of the vector separating the tanks
    float distanceVectMag = distanceVect.mag();

    // Minimum distance before the tank are touching border
    float minDistance = tank.radius * 2;
    
    // Skapa ett nytt SensorReading objekt.
    Sprite obj = new Sprite(); 
    obj.position = pvec; 
    
    
    SensorReading reading = new SensorReading(
      obj, 
      distanceVectMag - minDistance,
      angle);
    
    
    //return (distanceVectMag - minDistance);
    return reading;
  }

  //******************************************************
  /**
   * @return float array of size 1 with the current distance
   */
  // TODO this should fail to read based on the angle between ray and surface
  public float[] readValues() {
    PVector origin = tank.getRealPosition();
    //float direction = tank.getHeading() + (float) Math.toRadians(localOrientation);
    float direction = tank.getHeading();


    // use border of tank
    PVector v = PVector.fromAngle(direction);
    v.setMag(tank.getRadius());
    //origin.add(v);

    //float dist = game.closestSimulatableInRay(tank, origin, direction);
    
    //dist = getReadingAfterNoise(dist, NOISE_AMOUNT);
    float dist = 3.14f; // Dummy temp. 

    float[] values = new float[1];
    values[0] = Math.max(dist, 0);
    return values;
  }

}
/*
    This class uses Minim. When using Processing.js, we don't have 
    access to Minim so we have an equivalent class, SoundManager.js
    that handles audio.
*/
public class SoundManager{
  boolean muted = false;
  Minim minim;
  
  ArrayList <PlayerQueue> queuedSounds;
  ArrayList <String> queuedSoundNames;

  /*
      Handles the issue where we want to play multiple audio streams from the same clip.
  */
  private class PlayerQueue{
    private ArrayList <AudioPlayer> players;
    private String path;

    public PlayerQueue(String audioPath){

      path = audioPath;
      players = new ArrayList<AudioPlayer>();
      appendPlayer();
    }

    public void close(){
      for(int i = 0; i < players.size(); i++){
        players.get(i).close();
      }
    }

    public void play(){
      int freePlayerIndex = -1;
      for(int i = 0; i < players.size(); i++){
        if(players.get(i).isPlaying() == false){
          freePlayerIndex = i;
          break;
        }
      }

      if(freePlayerIndex == -1){
        appendPlayer();
        freePlayerIndex = players.size()-1;
      }

      players.get(freePlayerIndex).play();
      players.get(freePlayerIndex).rewind();
    }

    private void appendPlayer(){
      AudioPlayer player = minim.loadFile(path);
      players.add(player);
    }

    public void setMute(boolean m){
      for(int i = 0; i < players.size(); i++){
        if(m){
          players.get(i).mute();
        }
        else{
          players.get(i).unmute(); 
        }
      }
    }
  }
  
  /*
  */
  public SoundManager(PApplet applet){
    minim = new Minim(applet);

    queuedSounds = new ArrayList<PlayerQueue>();
    queuedSoundNames = new ArrayList<String>();
  }
  
  /*
  */
  public void setMute(boolean isMuted){
    muted = isMuted;

    for(int i = 0; i < queuedSounds.size(); i++){
      queuedSounds.get(i).setMute(muted);
    }
  }
  
  /*
  */
  public boolean isMuted(){
    return muted;
  }

  /*private void play(AudioPlayer player){
    if(muted || player.isPlaying()){
      return;
    }
    
    player.play();
    player.rewind();
  }*/
  
  /*
  */
  public void addSound(String soundName){
    //queuedSounds.add(new PlayerQueue("audio/" + soundName + ".wav"));
    queuedSounds.add(new PlayerQueue("audio/" + soundName + ".mp3"));
    queuedSoundNames.add(soundName);
  }

  /*
  */
  public void playSound(String soundName){
    if(muted){
      return;
    }

    int index = -1;

    for(int i = 0; i < queuedSoundNames.size(); i++){
      if(soundName.equals(queuedSoundNames.get(i))){
        index = i;
        break;
      }
    }

    if(index != -1){
      queuedSounds.get(index).play();
    }
  }
  
  /*
  */
  public void stop(){

    for(int i = 0; i < queuedSounds.size(); i++){
      queuedSounds.get(i).close();
    }

    minim.stop();
  }
}
class Sprite {
 
  PVector position;
  String name;
  float diameter, radius;
  
  //**************************************************
  public String getName(){
    return this.name;
  }
  
  //**************************************************
  public float diameter(){
    return this.diameter;
  }  
  
  //**************************************************
  public float getRadius(){
    return this.radius;
  }   
  
  //**************************************************
  public PVector position(){
    return this.position;
  }
  
}


class Tank extends Sprite { //<>//

  boolean[][] internalGrid;
  boolean searching = true;

  int id;
  //String name; //Sprite
  int team_id;

  PVector acceleration;
  PVector velocity;
  //PVector position; //Sprite
  boolean chill_state;

  float rotation;
  float rotation_speed;

  Node previousNode;

  boolean atHomeGoal;
  boolean retreating;

  Team team;
  PImage img;
  //float diameter; //Sprite
  //float radius; //Sprite

  float maxrotationspeed;
  float maxspeed;
  float maxforce;

  int health;// 3 är bra, 2 är mindre bra, 1 är immobilized, 0 är destroyed.
  boolean isImmobilized; // Tanken kan snurra på kanonen och skjuta, men inte förflytta sig.
  boolean isDestroyed; // Tanken är död.

  //PVector hitArea;

  PVector startpos;
  PVector positionPrev; //spara temp senaste pos.

  Node startNode; // noden där tanken befinner sig.

  boolean hasTarget; // Används just nu för att kunna köra "manuellt" (ai har normalt target).
  PVector targetPosition; // Används vid förflyttning mot target.
  float targetHeading; // Används vid rotation mot en target.
  PVector sensor_targetPosition;

  PVector[] otherTanks  = new PVector[5];
  PVector distance3_sensor;

  ArrayList listOfActions; // Används ännu inte.

  float heading; // Variable for heading!

  // variabler som anger vad tanken håller på med.
  boolean backward_state;
  boolean forward_state;
  boolean turning_right_state;
  boolean turning_left_state;
  boolean turning_turret_right_state;
  boolean turning_turret_left_state;
  boolean stop_state;
  boolean stop_turning_state;
  boolean stop_turret_turning_state;

  boolean idle_state; // Kan användas när tanken inte har nåt att göra.

  boolean isMoving; // Tanken är i rörelse.
  boolean isRotating; // Tanken håller på att rotera.
  boolean isColliding; // Tanken håller på att krocka.
  boolean isAtHomebase;
  boolean userControlled; // Om användaren har tagit över kontrollen.

  boolean hasShot; // Tanken kan bara skjuta om den har laddat kanonen, hasShot=true.
  CannonBall ball;

  float s = 2.0f;
  float image_scale;

  boolean isSpinning; // Efter träff snurrar tanken runt, ready=false.
  boolean isReady; // Tanken är redo för action efter att tidigare blivit träffad.
  int remaining_turns;
  float heading_saved; // För att kunna återfå sin tidigare heading efter att ha snurrat.

  Turret turret;

  // Tank sensors
  private HashMap<String, Sensor> mappedSensors = new HashMap<String, Sensor>();

  private ArrayList<Sensor> sensors = new ArrayList<Sensor>();
  public Set<Node> traversedNodes = new HashSet<Node>();
  public ArrayList<Node> total_path = new ArrayList<Node>();
  protected ArrayList<Sensor> mySensors = new ArrayList<Sensor>();
  public ArrayList<Node> homeBase = new ArrayList<Node>();

  //**************************************************
  Tank(int id, Team team, PVector _startpos, float diameter, CannonBall ball) {
    println("*** NEW TANK(): [" + team.getId()+":"+id+"]");
  
    this.id = id;
    this.team = team;
    this.team_id = this.team.getId();

    this.name = "tank";
    this.internalGrid = new boolean[grid.getRows()][grid.getCols()];
    this.startpos = new PVector(_startpos.x, _startpos.y);
    this.position = new PVector(this.startpos.x, this.startpos.y);
    this.velocity = new PVector(0, 0);

    this.acceleration = new PVector(0, 0);
    this.positionPrev = new PVector(this.position.x, this.position.y); //spara temp senaste pos.
    this.targetPosition = new PVector(this.position.x, this.position.y); // Tanks har alltid ett target.

    //this.startNode = grid.getNearestNodePosition(this.startpos);


    if (this.team.getId() == 0) this.heading = radians(0); // "0" radians.
    if (this.team.getId() == 1) this.heading = radians(180); // "3.14" radians.

    this.targetHeading = this.heading; // Tanks har alltid en heading mot ett target.
    this.hasTarget = false;

    this.diameter = diameter;
    this.radius = this.diameter/2; // For hit detection.
    this.retreating = false;
    this.backward_state = false;
    this.forward_state = false;
    this.turning_right_state = false;
    this.turning_left_state = false;
    this.turning_turret_right_state = false;
    this.turning_turret_left_state = false;
    this.stop_state = true;
    this.stop_turning_state = true;
    this.stop_turret_turning_state = true;
    // Under test
    this.isMoving = false;
    this.isRotating = false;
    this.isAtHomebase = true;
    this.idle_state = true;

    this.ball = ball;
    this.hasShot = false;
    this.maxspeed = 3; //3;
    this.maxforce = 0.1f;
    this.maxrotationspeed = radians(3);
    this.rotation_speed = 0;
    this.image_scale = 0.5f;
    this.isColliding = false;


    //this.img = loadImage("tankBody2.png");
    this.turret = new Turret(this.diameter/2);

    this.radius = diameter/2;

    this.health = 3;// 3 är bra, 2 är mindre bra, 1 är immobilized, 0 är oskadliggjord.
    this.isReady = true; // Tanken är redo för action.
    this.isImmobilized = false; // Tanken kan snurra på kanonen och skjuta, men inte förflytta sig.
    this.isDestroyed = false; // Tanken är död.

    this.isSpinning = false;
    this.remaining_turns = 0;
    this.heading_saved = this.heading;

    this.ball.setColor(this.team.getColor());
    this.ball.setOwner(this);


    initializeSensors();
  }

  public void reconstruct_path(Map<Node, Node> cameFrom, Node current) {
    System.out.println("RECONSTRUCT PATH");
      total_path.add(current);
    
      while (cameFrom.containsKey(current)) {
        current = cameFrom.get(current);
        total_path.add(current);
      }
      System.out.println(total_path.toString());
      //moveTo(this.total_path.get(this.total_path.size()-1).position);
    }

    public void traversePath(ArrayList<Node> nodeList){
      for (Node node : nodeList) {
        if (stop_state)
          moveTo(node.position);
      }
    }
    //A*
    public void calculatePath(Node startNode, Node goalNode){

      System.out.println("CALCULE PATH");
      //alla noder som är relevanta att kolla på
      Set<Node> openSet = new HashSet<Node>();
      openSet.add(startNode);
      Map<Node,Integer> gScore = new HashMap<Node,Integer>();
      gScore.put(startNode,0);
      Map<Node,Integer> fScore = new HashMap<Node,Integer>();
      fScore.put(startNode,heuristic(startNode,goalNode));
      Map<Node,Node> cameFrom = new HashMap<Node,Node>();

      while(!openSet.isEmpty()){
        System.out.println("openSet = " + openSet.size());
        int lowest = Integer.MAX_VALUE;
        Node current = null;
        for (Node node : openSet) {
          int value = fScore.get(node);

          if(value < lowest) {
            lowest = value;
            current = node;
          }
        }
        System.out.println(current.position.toString() +" : "+goalNode.position.toString());
        if(current.position.equals(goalNode.position)){
          System.out.println("goal found ------------");
          reconstruct_path(cameFrom, current);
          return;
        }

        openSet.remove(current);
        //VI ÄR HÄR DEN ADDERAR ALDRIG NEIGHBORS :(( (((())))))
        for(Node neighbor : grid.getNearestNodes(current)){
          if (traversedNodes.contains(neighbor)){
            System.out.println("Inside neighbor loop");
            if (!internalGrid[neighbor.getRow()][neighbor.getCol()]) {
              System.out.println("Not an obstacle ----------");
              int tentative_gScore = gScore.get(current) + 1;
              if(!gScore.containsKey(neighbor) || tentative_gScore < gScore.get(neighbor)){
                System.out.println("Inside gscore IF");
                cameFrom.put(neighbor, current);
                gScore.put(neighbor, tentative_gScore);
                fScore.put(neighbor, gScore.get(neighbor) + heuristic(neighbor, goalNode));
                if (!openSet.contains(neighbor)) {
                  System.out.println("Adding Neighbor!");
                  openSet.add(neighbor);
                }
              } 
            }
          }
        }
      }
    }
    //Calculate manhattan distance
    public int heuristic(Node node, Node goalNode){
      
      return Math.abs(node.getRow()-goalNode.getRow()) + Math.abs(node.getCol()-goalNode.getCol());
    }


  //**************************************************
  public int getId() {
    return this.id;
  }

  //**************************************************
  //String getName(){
  //  return this.name;
  //}

  //**************************************************
  public float getRadius() {
    return this.radius;
  }

  //**************************************************
  public float getHeadingInDegrees() {
    return degrees(this.heading);
  }

  //**************************************************
  public float getHeading() {
    return this.heading;
  }

  //**************************************************
  // Anropas då användaren tar över kontrollen av en tank.
  public void takeControl() {
    println("*** Tank[" + team.getId()+"].takeControl()");
    stopMoving_state();
    this.userControlled = true;
  }

  //**************************************************
  // Anropas då användaren släpper kontrollen av en tank.
  public void releaseControl() {
    println("*** Tank[" + team.getId()+"].releaseControl()");
    stopMoving_state();
    idle_state = true;

    this.userControlled = false;
  }

  //**************************************************
  // Används ännu inte.
  public PVector getRealPosition() {
    return this.position;
  }

  //************************************************** 
  // Returns the Sensor with the specified ID

  public Sensor getSensor(String ID) {
    return mappedSensors.get(ID);
  }

  //************************************************** 
  // Add your Sensor.

  public void addSensor(Sensor s) {
    mySensors.add(s);
  }

  //**************************************************
  //Register a sensor inside this robot, with the given ID

  protected void registerSensor(Sensor sensor, String ID) {
    mappedSensors.put(ID, sensor);
    sensors.add(sensor);
  }

  //**************************************************
  protected void initializeSensors() {

    SensorDistance ultrasonic_front = new SensorDistance(this, 0f);
    registerSensor(ultrasonic_front, "ULTRASONIC_FRONT");

    //SensorDistance ultrasonic_back = new SensorDistance(this, 180f);
    //registerSensor(ultrasonic_back, "ULTRASONIC_BACK");

    /*
     SensorCompass compass = new SensorCompass(game, this);
     registerSensor(compass, "COMPASS");
     
     SensorDistance ultrasonic_left = new SensorDistance(game, this, 270f);
     registerSensor(ultrasonic_left, "ULTRASONIC_LEFT");
     
     SensorDistance ultrasonic_right = new SensorDistance(game, this, 90f);
     registerSensor(ultrasonic_right, "ULTRASONIC_RIGHT");
     
     SensorDistance ultrasonic_front = new SensorDistance(game, this, 0f);
     registerSensor(ultrasonic_front, "ULTRASONIC_FRONT");
     
     SensorDistance ultrasonic_back = new SensorDistance(game, this, 180f);
     registerSensor(ultrasonic_back, "ULTRASONIC_BACK");
     */
  }

  //**************************************************

  public SensorReading readSensor_distance(Sensor sens) {
    //println("*** Tank.readSensorDistance()");

    Sprite df = sens.readValue().obj();

    return sens.readValue();
  }

  //**************************************************
  public void readSensors() {
    /*
     println("*** Tank[" + team.getId()+"].readSensors()");
     println("sensors: " + sensors);
     
     for (Sensor s : mySensors) {
     if (s.tank == this) {
     PVector sens = (s.readValue().obj().position);
     
     //println("============");
     //println("("+sens.x + " , "+sens.y+")");
     //ellipse(sens.x, sens.y, 10,10);
     if (sens != null) {
     line(this.position.x,this.position.y, sens.x, sens.y);
     println("Tank" + this.team.getId() + ":"+this.id + " ( " + sens.x + ", "+ sens.y + " )");
     
     }
     }
     */
  }

  //**************************************************
  public void spin(int antal_varv) {
    println("*** Tank[" + team.getId()+"].spin(int)");
    if (!this.isSpinning) {
      this.heading_saved = this.heading;
      isSpinning = true; 
      this.remaining_turns = antal_varv;
    }
  }

  //**************************************************
  // After calling this method, the tank can shoot.
  public void loadShot() {
    println("*** Tank[" + team.getId()+":"+id+"].loadShot() and ready to shoot.");

    this.hasShot = true;
    this.ball.loaded();
  }

  //**************************************************
  public void testCollisionSensor() {
  }

  //**************************************************
  public void fire() {
    // Ska bara kunna skjuta när den inte rör sig.
    if (this.stop_state) {
      println("*** Tank[" + this.team.getId()+":"+this.id+"].fire()");

      if (this.hasShot) {
        println("! Tank["+ this.getId() + "] – PANG.");
        this.hasShot = false;

        PVector force = PVector.fromAngle(this.heading + this.turret.heading);
        force.mult(10);
        this.ball.applyForce(force);

        shoot(this.id); // global funktion i huvudfilen

        soundManager.playSound("tank_firing");
        //soundManager.playSound("blast");
      } else {
        println("! Tank["+ this.getId() + "] – You have NO shot loaded and ready.");
      }
    } else {
      println("! Tank["+ this.getId() + "] – The tank must stand STILL to shoot.");
    }
  }

  //**************************************************
  // Anropad från den cannonBall som träffat.
  public final boolean takeDamage() {
    println("*** Tank["+ this.getId() + "].takeDamage()");

    if (!this.isDestroyed) {
      this.health -= 1;

      println("! Tank[" + team.getId()+":"+id+"] has been hit, health is now "+ this.health);

      stopMoving_state();
      resetTargetStates();

      if (!this.isImmobilized) {
        if (this.health == 1) {
          this.isImmobilized = true;
        }
      }

      if (this.health <= 0) {
        this.health = 0;
        this.isDestroyed = true;
        this.isSpinning  = false;
        this.isReady = false;

        return true;
      }

      spin(3);
      this.isReady = false; // Efter träff kan inte tanken utföra action, så länge den "snurrar".
      return true;
    }

    return false; // ingen successfulHit omtanken redan är destroyed.
  }

  //**************************************************
  // Anropad från sin egen cannonBall efter träff.
  public final void successfulHit() {
    this.team.messageSuccessfulHit();
  }

  //**************************************************
  // Det är denna metod som får tankens kanon att svänga vänster.
  public void turnTurretLeft_state() {
    if (this.stop_state) { 
      if (!this.turning_turret_left_state) {
        println("*** Tank[" + getId() + "].turnTurretLeft_state()");
        this.turning_turret_right_state = false;
        this.turning_turret_left_state = true;
        this.stop_turret_turning_state = false;
      }
    } else {
      println("Tanken måste stå still för att kunna rotera kanonen.");
    }
  }

  //**************************************************
  public void turnTurretLeft() {
    this.turret.turnLeft();
  }

  //**************************************************
  // Det är denna metod som får tankens kanon att svänga höger.
  public void turnTurretRight_state() {
    if (this.stop_state) { 
      if (!this.turning_turret_right_state) {
        println("*** Tank[" + getId() + "].turnTurretRight_state()");
        this.turning_turret_left_state = false;
        this.turning_turret_right_state = true;
        this.stop_turret_turning_state = false;
      }
    } else {
      println("Tanken måste stå still för att kunna rotera kanonen.");
    }
  }

  //**************************************************
  public void turnTurretRight() {
    this.turret.turnRight();
  }

  //**************************************************
  // Det är denna metod som får tankens kanon att sluta rotera.
  public void stopTurretTurning_state() {
    if (!this.stop_turret_turning_state) {
      println("*** Tank[" + getId() + "].stopTurretTurning_state()");
      this.turning_turret_left_state = false;
      this.turning_turret_right_state = false;
      this.stop_turret_turning_state = true;
    }
  }

  //**************************************************
  // Det är denna metod som får tanken att svänga vänster.
  public void turnLeft_state() {
    this.stop_turning_state = false;
    this.turning_right_state = false;

    if (!this.turning_left_state) {
      println("*** Tank[" + getId() + "].turnLeft_state()");
      this.turning_left_state = true;
    }
  }

  //**************************************************
  public void turnLeft() {
    //println("*** Tank[" + getId()+"].turnLeft()");

    if (this.hasTarget && abs(this.targetHeading - this.heading) < this.maxrotationspeed) {
      this.rotation_speed -= this.maxforce;
    } else {
      this.rotation_speed += this.maxforce;
    }
    if (this.rotation_speed > this.maxrotationspeed) {
      this.rotation_speed = this.maxrotationspeed;
    }
    this.heading -= this.rotation_speed;
  }

  //**************************************************
  // Det är denna metod som får tanken att svänga höger.
  public void turnRight_state() {
    this.stop_turning_state = false;
    this.turning_left_state = false;

    if (!this.turning_right_state) {
      println("*** Tank[" + getId() + "].turnRight_state()");
      this.turning_right_state = true;
    }
  }

  //**************************************************  
  public void turnRight() {
    //println("*** Tank[" + getId() + "].turnRight()");

    if (this.hasTarget && abs(this.targetHeading - this.heading) < this.maxrotationspeed) {
      this.rotation_speed -= this.maxforce;
    } else {
      this.rotation_speed += this.maxforce;
    }
    if (this.rotation_speed > this.maxrotationspeed) {
      this.rotation_speed = this.maxrotationspeed;
    }
    this.heading += this.rotation_speed;
  }

  //**************************************************  
  public void turnRight(PVector targetPos) {
    println("*** Tank[" + getId() + "].turnRight(PVector)");
    PVector desired = PVector.sub(targetPos, position);  // A vector pointing from the position to the target

    desired.setMag(0);
    PVector steer = PVector.sub(desired, velocity);
    steer.limit(maxforce);  // Limit to maximum steering force
    applyForce(steer);

  }

  //**************************************************  
  public void stopTurning() {
    println("*** Tank[" + getId()+"].stopTurning()");
    this.rotation_speed = 0;
    arrivedRotation();
  }

  //**************************************************
  // Det är denna metod som får tanken att sluta svänga.
  public void stopTurning_state() {
    if (!this.stop_turning_state) {
      println("*** Tank[" + getId() + "].stopTurning_state()");
      this.turning_left_state = false;
      this.turning_right_state = false;
      this.stop_turning_state = true;

      println("! Tank[" + getId() + "].stopTurning_state() – stop_turning_state=true");
    }
  }

  //**************************************************
  public void moveTo(float x, float y) {
    println("*** Tank["+ this.getId() + "].moveTo(float x, float y)");

    moveTo(new PVector(x, y));
  }

  //**************************************************
  public void moveTo(PVector coord) {
    //println("*** Tank["+ this.getId() + "].moveTo(PVector)");
    if (!isImmobilized) {
      println("*** Tank["+ this.getId() + "].moveTo(PVector)");

      this.idle_state = false;
      this.isMoving = true;
      this.stop_state = false;

      this.targetPosition.set(coord);
      this.hasTarget = true;
    }
  }

  //**************************************************
  public void moveBy(float x, float y) {
    println("*** Tank["+ this.getId() + "].moveBy(float x, float y)");

    moveBy(new PVector(x, y));
  }

  //**************************************************
  public void moveBy(PVector coord) {
    println("*** Tank["+ this.getId() + "].moveBy(PVector)");

    PVector newCoord = PVector.add(this.position, coord);
    PVector nodevec = grid.getNearestNodePosition(newCoord);

    moveTo(nodevec);
  }

  //**************************************************
  // Det är denna metod som får tanken att gå framåt.
  public void moveForward_state() {
    println("*** Tank[" + getId() + "].moveForward_state()");

    if (!this.forward_state) {
      this.acceleration.set(0, 0, 0);
      this.velocity.set(0, 0, 0);

      this.forward_state = true;
      this.backward_state = false;
      this.stop_state = false;
    }
  }

  //**************************************************
  public void moveForward() {
    //println("*** Tank[" + getId() + "].moveForward()");

    // Offset the angle since we drew the ship vertically
    float angle = this.heading; // - PI/2;
    // Polar to cartesian for force vector!
    PVector force = new PVector(cos(angle), sin(angle));
    force.mult(0.1f);
    applyForce(force);
  }

  //**************************************************
  public void moveForward(int numSteps) {
  }

  //**************************************************
  // Det är denna metod som får tanken att gå bakåt.
  public void moveBackward_state() {
    println("*** Tank[" + getId() + "].moveBackward_state()");
    this.stop_state = false;
    this.forward_state = false;

    if (!this.backward_state) {
      println("! Tank[" + getId() + "].moveBackward_state() – (!this.backward_state)");
      this.acceleration.set(0, 0, 0);
      this.velocity.set(0, 0, 0);
      this.backward_state = true;
    }
  }

  //**************************************************
  public void moveBackward() {
    println("*** Tank[" + getId() + "].moveBackward()");
    // Offset the angle since we drew the ship vertically
    float angle = this.heading - PI; // - PI/2;
    // Polar to cartesian for force vector!
    PVector force = new PVector(cos(angle), sin(angle));
    force.mult(0.1f);
    applyForce(force);
  }

  //**************************************************
  public void stopMoving() {
    println("*** Tank[" + getId() + "].stopMoving()");

    this.acceleration.set(0, 0, 0);
    this.velocity.set(0, 0, 0);

    this.isMoving = false;  

    resetTargetStates();
  }

  //**************************************************
  // Det är denna metod som får tanken att sluta åka framåt eller bakåt.
  // "this.stop_state" anropas 
  public void stopMoving_state() {
    //println("stopMoving_state() ");

    if (!this.stop_state) {
      //println("*** Tank[" + getId() + "].stopMoving_state()");

      resetMovingStates();
      stopMoving();
    }
  }

  //**************************************************
  public void resetAllMovingStates() {
    println("*** Tank[" + getId() + "].resetAllMovingStates()");
    this.stop_state = true;
    this.backward_state = false;
    this.forward_state = false;

    this.backward_state = false;
    this.forward_state = false;
    this.turning_right_state = false;
    this.turning_left_state = false;
    this.turning_turret_right_state = false;
    this.turning_turret_left_state = false;
    this.stop_state = true;
    this.stop_turning_state = true;
    this.stop_turret_turning_state = true;

    this.velocity = new PVector(0, 0);
    this.acceleration = new PVector(0, 0);
  }

  //**************************************************
  public void resetMovingStates() {
    println("*** Tank[" + getId() + "].resetMovingStates()");
    this.stop_state = true;
    this.backward_state = false;
    this.forward_state = false;
  }

  //**************************************************
  public void resetTargetStates() {
    println("*** Tank[" + getId() + "].resetTargetStates()");
    this.targetPosition = new PVector(this.position.x, this.position.y);

    this.targetHeading = this.heading; // Tanks har alltid en heading mot ett target.
    this.hasTarget = false;
  }

  //**************************************************
  public void updatePosition() {

    this.positionPrev.set(this.position); // spara senaste pos.

    this.velocity.add(this.acceleration);
    this.velocity.limit(this.maxspeed);
    this.position.add(this.velocity);
    this.acceleration.mult(0);
  }

  //**************************************************
  // Newton's law: F = M * A
  public void applyForce(PVector force) {
    this.acceleration.add(force);
  }

  //**************************************************
  public void destroy() {
    println("*** Tank.destroy()");
    //dead = true;
    this.isDestroyed = true;
  }

  //**************************************************

  public void rotating() {
    //println("*** Tank["+ this.getId() + "].rotating()");
    if (!isImmobilized) {

      if (this.hasTarget) {
        float diff = this.targetHeading - this.heading;

        if ((abs(diff) <= radians(0.5f))) {
          this.isRotating = false;
          this.heading = this.targetHeading;
          this.targetHeading = 0.0f;
          this.hasTarget = false;
          stopTurning_state();
          arrivedRotation();
        } else if ((diff) > radians(0.5f)) {

          turnRight_state();
        } else if ((diff) < radians(0.5f)) {
          turnLeft_state();
        }
      }
    }
  }

  //**************************************************

  public void rotateTo(float angle) {
    println("*** Tank["+ this.getId() + "].rotateTo(float): "+angle);

    if (!isImmobilized) {

      this.heading = angle;

      // Hitta koordinaten(PVector) i tankens riktning
      Sensor sens = getSensor("ULTRASONIC_FRONT");
      PVector sens_pos = (sens.readValue().obj().position);
      PVector grid_pos = grid.getNearestNodePosition(sens_pos);
      rotateTo(grid_pos); // call "rotateTo(PVector)"
    }
  }

  //**************************************************

  public void rotateTo(PVector coord) {
    println("*** Tank["+ this.getId() + "].rotateTo(PVector) – ["+(int)coord.x+","+(int)coord.y+"]");

    if (!isImmobilized) {

      this.idle_state = false;
      this.isMoving = false;
      this.isRotating = true;
      this.stop_state = false;
      this.hasTarget = true;


      PVector target = new PVector(coord.x, coord.y);
      PVector me = new PVector(this.position.x, this.position.y);

      // Bestäm headin till target.
      PVector t = PVector.sub(target, me);
      this.targetHeading = t.heading();
    }
  }

  //**************************************************
  // A method that calculates a steering force towards a target
  // STEER = DESIRED MINUS VELOCITY
  public void arrive() {

    // rotera tills heading mot target.
    PVector desired = PVector.sub(this.targetPosition, this.position);  // A vector pointing from the position to the target
    float d = desired.mag();
    // If arrived

    // Scale with arbitrary damping within 100 pixels
    
    if (d < 20) {
      float m = map(d, 0, 20, 0, maxspeed);
      desired.setMag(m);
    } else {
      desired.setMag(maxspeed);
    }
  

    // Steering = Desired minus Velocity
    PVector steer = PVector.sub(desired, velocity);
    steer.limit(maxforce);  // Limit to maximum steering force
    applyForce(steer);

    if (d < 1) {
      arrived();
    }
  }

  //**************************************************
  // Tanken meddelas om att tanken är redo efter att blivit träffad.
  public void readyAfterHit() {
    println("*** Tank["+ this.getId() + "].readyAfterHit()");

    if (!this.isDestroyed) {
      this.isReady = true; // Efter träff kan inte tanken utföra action, så länge den "snurrar".
    }
  }

  //**************************************************
  // Tanken meddelas om kollision med trädet.
  public void arrivedRotation() {
    println("*** Tank["+ this.getId() + "].arrivedRotation()");
    stopTurning_state();
    this.isMoving = false;
  }

  //**************************************************
  public void arrived() {
    println("*** Tank["+ this.getId() + "].arrived()");
    this.isMoving = false;  
    stopMoving_state();
  }

  //**************************************************
  // Är tänkt att överskuggas (override) i subklassen.
  public void updateLogic() {
  }

  //**************************************************
  // Called from game
  public final void update() {

    // Om tanken fortfarande lever.
    if (!this.isDestroyed) {
      // Om tanken har blivit träffad och håller på och snurrar runt.
      int spinning_speed = 5;
      if (this.isSpinning) {
        if (this.remaining_turns > 0) {
          this.heading += rotation_speed * spinning_speed; 

          if (this.heading > (this.heading_saved + (2 * PI))||(this.heading == this.heading_saved)) {

            this.remaining_turns -= 1;
            this.heading = this.heading_saved;
          }
        } else {

          this.heading = this.heading_saved;
          this.remaining_turns = 0;
          this.isSpinning = false;
          this.isReady = true;

          this.idle_state = true;
        }
      } else {

        // Om tanken är redo för handling och kan agera.
        if (!this.isImmobilized && this.isReady) {  

          // Om tanken är i rörelse.
          if (this.isMoving) {

            this.heading = this.velocity.heading();
            arrive();
          }


          // Om tanken ska stanna, men ännu inte gjort det.
          if (this.stop_state && !this.idle_state) {

            resetTargetStates(); // Tank
            resetAllMovingStates(); // Tank 
            this.idle_state = true;

            println("! Tank[" + getId() + "].update() – idle_state = true");
          }

          // Om tanken håller på och rotera.
          if (this.isRotating) {
            rotating();
          }

          // ----------------
          // state-kontroller
          if (this.forward_state) {
            moveForward();
          }
          if (this.backward_state) {
            moveBackward();
          }
          if (this.turning_right_state) {
            turnRight();
          }
          if (this.turning_left_state) {
            turnLeft();
          }

          if (this.stop_state && !this.isMoving && this.hasTarget) {
            println("Tank["+ this.getId() + "], vill stanna!");
            //this.stop_state = false;
            stopMoving();
          }
          if (this.stop_turning_state && !this.isMoving && this.hasTarget) {
            println("Tank["+ this.getId() + "], vill sluta rotera!");
            stopTurning();
          }
        } // end (!this.isImmobilized && this.isReady)



        // Om tanken är immobilized
        // Om tanken har laddat ett skott.
        if (this.hasShot) {
          this.ball.updateLoadedPosition(this.position);
        }

        //---------------
        // state-kontroller ...
        if (this.turning_turret_left_state) {
          turnTurretLeft();
        }
        if (this.turning_turret_right_state) {
          turnTurretRight();
        }


        readSensors();
      }
      updatePosition();
    }
  }

  //**************************************************
  // Anropas från spelet.
  public void checkEnvironment() {
    //checkEnvironment_sensor();

    // Check for collisions with Canvas Boundaries
    float r = this.diameter/2;
    if ((this.position.y+r > height) || (this.position.y-r < 0) ||
      (this.position.x+r > width) || (this.position.x-r < 0)) {
      if (!this.stop_state) {
        this.position.set(this.positionPrev); // Flytta tillbaka.
        //println("***");
        stopMoving_state();
      }
    }

    if (
      position.x > team.homebase_x && 
      position.x < team.homebase_x+team.homebase_width &&
      position.y > team.homebase_y &&
      position.y < team.homebase_y+team.homebase_height) {
      if (!isAtHomebase) {
        isAtHomebase = true;
        message_arrivedAtHomebase();
      }
    } else {
      isAtHomebase = false;
    }
  }

  // Tanken meddelas om att tanken är i hembasen.
  public void message_arrivedAtHomebase() {
    println("! Tank["+ this.getId() + "] – har kommit hem.");
  }

  // Tanken meddelas om kollision med trädet.
  public void message_collision(Tree other) {
    println("*** Tank["+ this.getId() + "].collision(Tree)");
    //println("Tank.COLLISION");
  }

  // Tanken meddelas om kollision med den andra tanken.
  public void message_collision(Tank other) {
    println("*** Tank["+ this.getId() + "].collision(Tank)");
    //println("Tank.COLLISION");
  }

  public void collideTree(PVector collisionPosition){
    //Om den man krockade med inte är den man vill gå till
    if(!collisionPosition.equals(targetPosition)){
      calculatePath(this.previousNode, grid.getNearestNode(targetPosition));
      searching = false;
    }
  }

  //*************************************************
  public void collide(PVector collisionPosition, boolean enemy){
    if(!collisionPosition.equals(targetPosition)){
      this.searching = false;
      if (enemy) {
        traversedNodes.add(this.previousNode);
        traversedNodes.add(grid.getNearestNode(collisionPosition));
        for (Node node : grid.getNearestNodes(new Node(collisionPosition.x, collisionPosition.y))) {
          if (!traversedNodes.contains(node)) {
            traversedNodes.add(node);
          }
        }
        addStationaryTankObstacle(targetPosition);
        retreating = true;
        Node goalNode = null;
        float currentDistance = 10000;
        for (Node node : homeBase) {
          PVector distance = PVector.sub(node.position, this.position);
          float distanceVectMag = distance.mag();
          if (distanceVectMag < currentDistance) {
            goalNode = node;
            currentDistance = distanceVectMag;
          } 
        }
        System.out.println("GOING HOME TO ----------:" + goalNode.position.x + goalNode.position.y);
        calculatePath(grid.getNearestNode(position), grid.getNearestNode(goalNode.position));
        
      } else {
        calculatePath(grid.getNearestNode(position), grid.getNearestNode(targetPosition));

      }
    }
  }


  public void checkCollision(Tree other) {
    //println("*** Tank.checkCollision(Tree)");
    // Check for collisions with "no Smart Objects", Obstacles (trees, etc.)
    // Get distances between the tree component
    PVector distanceVect = PVector.sub(other.position, this.position);

    // Calculate magnitude of the vector separating the tank and the tree
    float distanceVectMag = distanceVect.mag();

    // Minimum distance before they are touching
    float minDistance = this.radius + other.radius;

    if (distanceVectMag <= minDistance && !this.stop_state) {
      addTreeObstacle(other, this.targetPosition);
      addStationaryTankObstacle(other.position);
      collideTree(other.position);
      this.position.set(this.positionPrev);
      println("! Tank["+ this.getId() + "] – collided with Tree.");

      if (!this.stop_state) {
       
        //this.position.set(this.positionPrev); // Flytta tillbaka.

        // Kontroll om att tanken inte "fastnat" i en annan tank. 
        distanceVect = PVector.sub(other.position, this.position);
        distanceVectMag = distanceVect.mag();
        if (distanceVectMag < minDistance) {
          
          println("! Tank["+ this.getId() + "] – FAST I ETT TRÄD");
        }
        this.isMoving = false;  
        stopMoving_state();
      }

      if (this.hasShot) {
        this.ball.updateLoadedPosition(this.positionPrev);
      }


      // Meddela tanken om att kollision med trädet gjorts.
      message_collision(other);//collision(Tree);
    }
  }

  public void addTreeObstacle(Tree tree, PVector targetedPosition){
      PVector distanceVect = PVector.sub(tree.position, targetedPosition);
      float distanceVectMag = distanceVect.mag();
      if(distanceVectMag < (tree.getRadius()+this.getRadius())) {
        Node node = grid.getNearestNode(targetPosition);
        internalGrid[node.getRow()][node.getCol()] = true;
      }

    }

    public void addStationaryTankObstacle(PVector position){
      Node node = grid.getNearestNode(position);
      internalGrid[node.getRow()][node.getCol()] = true;

    }

  //**************************************************
  // Called from environment
  // Keeps an array with vectors to the other tanks, so the tank object can access the other tanks when called for.
  public void checkCollision(Tank other) {
    //println("*** Tank.checkCollision(Tank)");
    // Check for collisions with "Smart Objects", other Tanks.
    // Get distances between the tanks components
    PVector distanceVect = PVector.sub(other.position, this.position);

    boolean enemy = false;
    if (other.team_id != team_id) {
      enemy = true;
      for (Node node : grid.getNearestNodes(new Node(other.position.x, other.position.y))) {
        internalGrid[node.getRow()][node.getCol()] = true;
      }
    }
    // Calculate magnitude of the vector separating the tanks
    float distanceVectMag = distanceVect.mag();

    // Minimum distance before they are touching
    float minDistance = this.radius + other.radius;

    if (distanceVectMag <= minDistance) {
      //Backa ett steg
      //traversedNodes.add(grid.getNearestNode(other.position));
      
      println("! Tank["+ this.getId() + "] – collided with another Tank" + other.team_id + ":"+other.id);
      addStationaryTankObstacle(other.position);
      if (!this.stop_state) {
        //this.position.set(this.positionPrev); // Flytta tillbaka.

        // Kontroll om att tanken inte "fastnat" i en annan tank. 
        distanceVect = PVector.sub(other.position, this.position);
        distanceVectMag = distanceVect.mag();


        if (distanceVectMag <= minDistance) {
          this.position.set(this.positionPrev);
          collide(other.position, enemy);
          println("! Tank["+ this.getId() + "] – FAST I EN ANNAN TANK");
        }
        this.isMoving = false;  
        stopMoving_state();
      }

      if (this.hasShot) {
        this.ball.updateLoadedPosition(this.positionPrev);
      }


      // Meddela tanken om att kollision med den andra tanken gjorts.
      message_collision(other);
    }
  }

  public void setNode() {
    //setTargetPosition(this.position);
  }

  public void displayInfo() {
    fill(230);
    rect(width - 151, 0, 150, 300);
    strokeWeight(1);
    fill(255, 0, 0);
    stroke(255, 0, 0);
    textSize(10);
    text("id: "+this.id+"\n"+
      "health: "+this.health+"\n"+
      "position: ("+(int)this.position.x +","+(int)this.position.y+")"+"\n"+
      "isMoving: "+this.isMoving+"\n"+
      "isSpinning : "+this.isSpinning +"\n"+
      "remaining_turns: "+this.remaining_turns +"\n"+
      "isReady : "+this.isReady +"\n"+
      "hasTarget : "+this.hasTarget +"\n"+
      "stop_state : "+this.stop_state +"\n"+
      "stop_turning_state : "+this.stop_turning_state +"\n"+
      "idle_state : "+this.idle_state +"\n"+
      "isDestroyed : "+this.isDestroyed +"\n"+
      "isImmobilized : "+this.isImmobilized +"\n"+
      "targetHeading : "+this.targetHeading +"\n"+
      "heading : "+this.heading +"\n"+
      "heading_saved: "+this.heading_saved +"\n"
      , width - 145, 35 );
  }

  //**************************************************
  public void drawTank(float x, float y) {
    fill(this.team.getColor()); 

    if (this.team.getId() == 0) fill((((255/6) * this.health) *40 ), 50* this.health, 50* this.health, 255 - this.health*60);
    if (this.team.getId() == 1) fill(10*this.health, (255/6) * this.health, (((255/6) * this.health) * 3), 255 - this.health*60);

    if (this.userControlled) {
      strokeWeight(3);
    } else strokeWeight(1);

    ellipse(x, y, 50, 50);
    strokeWeight(1);
    line(x, y, x+25, y);

    fill(this.team.getColor(), 255); 
    this.turret.display();
  }

  //**************************************************
  public final void display() {

    imageMode(CENTER);
    pushMatrix();
    translate(this.position.x, this.position.y);

    rotate(this.heading);

    //image(img, 20, 0);
    drawTank(0, 0);

    if (debugOn) {
      noFill();
      strokeWeight(2);
      stroke(255, 0, 0);
      ellipse(0, 0, this.radius * 2, this.radius * 2);

      //for (Sensor s : mySensors) {
      //  if (s.tank == this) {
      //     strokeWeight(2);
      //     stroke(0,0,255); 
      //     PVector sens = s.readValue1();
      //     println("============");
      //     println("("+sens.x + " , "+sens.y+")");
      //     //ellipse(sens.x, sens.y, 10,10);
      //  }
      //}
    }

    popMatrix();  

    if (pause) {
      PVector mvec = new PVector(mouseX, mouseY);
      PVector distanceVect = PVector.sub(mvec, this.position);
      float distanceVectMag = distanceVect.mag();
      if (distanceVectMag < getRadius()) {
        displayInfo();
      }
    }

    if (debugOn) {

      for (Sensor s : mySensors) {
        if (s.tank == this) {
          // Rita ut vad sensorn ser (target och linje dit.)
          strokeWeight(1);
          stroke(0, 0, 255); 
          PVector sens = (s.readValue().obj().position);

          //println("============");
          //println("("+sens.x + " , "+sens.y+")");
          //ellipse(sens.x, sens.y, 10,10);

          if ((sens != null && !this.isSpinning && !isImmobilized)) {
            line(this.position.x, this.position.y, sens.x, sens.y);
            ellipse(sens.x, sens.y, 10, 10);
            //println("Tank" + this.team.getId() + ":"+this.id + " ( " + sens.x + ", "+ sens.y + " )");
          }
        }
      }

      // Rita ut en linje mot target, och tank-id och tank-hälsa.
      strokeWeight(2);
      fill(255, 0, 0);
      stroke(255, 0, 0);
      textSize(14);
      text(this.id+":"+this.health, this.position.x + this.radius, this.position.y + this.radius);

      if (this.hasTarget) {
        strokeWeight(1);
        line(this.position.x, this.position.y, this.targetPosition.x, targetPosition.y);
      }
    }
  }
}
class Team {

  Tank[] tanks = new Tank[3];
  int id; // team red 0, team blue 1.
  int tank_size;
  PVector tank0_startpos = new PVector();
  PVector tank1_startpos = new PVector();
  PVector tank2_startpos = new PVector();

  float homebase_x;
  float homebase_y;
  float homebase_width = 150;
  float homebase_height = 350;

  int team_color;

  int numberOfHits; // sammalagda antalet bekräftade träffar på andra lagets tanks. 


  Team (int team_id, int tank_size, int c, 
    PVector tank0_startpos, int tank0_id, CannonBall ball0, 
    PVector tank1_startpos, int tank1_id, CannonBall ball1, 
    PVector tank2_startpos, int tank2_id, CannonBall ball2) 
  {
    this.id = team_id;
    this.tank_size = tank_size;
    this.team_color = c;
    this.tank0_startpos.set(tank0_startpos);
    this.tank1_startpos.set(tank1_startpos);
    this.tank2_startpos.set(tank2_startpos);

    this.numberOfHits = 0; 

    tanks[0] = new Tank(tank0_id, this, this.tank0_startpos, this.tank_size, ball0);
    tanks[1] = new Tank(tank1_id, this, this.tank1_startpos, this.tank_size, ball1);
    tanks[2] = new Tank(tank2_id, this, this.tank2_startpos, this.tank_size, ball2);
    
    
    if (this.id==0) {this.homebase_x = 0; this.homebase_y = 0;}
    else if (this.id==1) {this.homebase_x = width - 151; this.homebase_y = height - 351;}
    
  }

  public int getId() {
    return this.id;
  }

  public int getColor() {
    return this.team_color;
  }

  public void messageSuccessfulHit() {
    this.numberOfHits += 1;
  }
  public void reportedEnemyTank(){
    this.numberOfHits +=1;
  }

  public void updateLogic() {

  }


  // Används inte.
  // Hemma i homebase
  //boolean isInHomebase(PVector pos) {
  //  return true;
  //}
  
  public void displayHomeBaseTeam() {
    strokeWeight(1);
    //fill(204, 50, 50, 15);
    fill(this.team_color, 15);
    //rect(0, 0, 150, 350);
    rect(this.homebase_x, this.homebase_y, this.homebase_width, this.homebase_height);
  }
  

  public void displayHomeBase(){
    displayHomeBaseTeam();
  }
  
}



class Team1 extends Team {

  Team1(int team_id, int tank_size, int c, 
    PVector tank0_startpos, int tank0_id, CannonBall ball0, 
    PVector tank1_startpos, int tank1_id, CannonBall ball1, 
    PVector tank2_startpos, int tank2_id, CannonBall ball2) {
    super(team_id, tank_size, c, tank0_startpos, tank0_id, ball0, tank1_startpos, tank1_id, ball1, tank2_startpos, tank2_id, ball2);  

    tanks[0] = new Tank2(tank0_id, this, this.tank0_startpos, this.tank_size, ball0);
    tanks[1] = new Tank(tank1_id, this, this.tank1_startpos, this.tank_size, ball1);
    tanks[2] = new Tank(tank2_id, this, this.tank2_startpos, this.tank_size, ball2);

    //this.homebase_x = 0;
    //this.homebase_y = 0;
  }

  //==================================================
  public class Tank2 extends Tank {
    
    boolean started;
    Stack<Node> nodeStack = new Stack<Node>();

    //*******************************************************
    Tank2(int id, Team team, PVector startpos, float diameter, CannonBall ball) {
      super(id, team, startpos, diameter, ball);

      this.started = false; 

      //this.isMoving = true;
      //moveTo(grid.getRandomNodePosition());
    }

    //*******************************************************
    // Reterera, fly!
    public void retreat() {
      println("*** Team"+this.team_id+".Tank["+ this.getId() + "].retreat()");
      moveTo(grid.getRandomNodePosition()); // Slumpmässigt mål.
    }

    //*******************************************************
    // Reterera i motsatt riktning (ej implementerad!)
    public void retreat(Tank other) {
      //println("*** Team"+this.team_id+".Tank["+ this.getId() + "].retreat()");
      //moveTo(grid.getRandomNodePosition());
      retreat();
    }


    //*******************************************************
    // Fortsätt att vandra runt.
    public void wander() {
      if(this.searching){
        Node currentNode = this.nodeStack.pop();
        if (!traversedNodes.contains(currentNode))
          moveTo(currentNode.position);
        

        //om current node inte finns i traversed node, lägg till den
        if(!this.traversedNodes.contains(currentNode)){
          this.traversedNodes.add(currentNode);
          
          //För varje närliggande nod pusha den noden till stacken.
          for(Node node : grid.getNearestNodes(currentNode)){
            if(!this.traversedNodes.contains(node)){
                this.nodeStack.push(node);
            }
          }
        }

      } else if (!this.total_path.isEmpty()){
        // Traversera den, med A-stjärna, uträknade vägen.
        moveTo(this.total_path.get(this.total_path.size()-1).position);
        this.total_path.remove(this.total_path.size()-1);
        
        
        
      }else{
        if(this.retreating && total_path.isEmpty()){
          new Thread(new Runnable() {
            @Override
            public void run() {
              waitInBase();
            }
          }).start(); 
        }
        if(!this.chill_state){
            this.searching = true;
        }
        
        
      }
    }
    public void waitInBase(){
      System.out.println("Waiting in base for 3 seconds!");

      int currentMS = millis();

      while(millis() < currentMS+3000){
        this.chill_state = true;
        this.stop_state = true;
      }
      this.chill_state = false;
      retreating = false;
      team.reportedEnemyTank();
      this.searching = true;
    }

    //*******************************************************
    // Tanken meddelas om kollision med trädet.
    public void message_collision(Tree other) {
      println("*** Team"+this.team_id+".Tank["+ this.getId() + "].collision(Tree)");
      wander();
    }

    //*******************************************************
    // Tanken meddelas om kollision med tanken.
    public void message_collision(Tank other) {
      println("*** Team"+this.team_id+".Tank["+ this.getId() + "].collision(Tank)");

      //moveTo(new PVector(int(random(width)),int(random(height))));
      //println("this.getName());" + this.getName()+ ", this.team_id: "+ this.team_id);
      //println("other.getName());" + other.getName()+ ", other.team_id: "+ other.team_id);

      if ((other.getName() == "tank") && (other.team_id != this.team_id)) {
        if (this.hasShot && (!other.isDestroyed)) {
          println("["+this.team_id+":"+ this.getId() + "] SKJUTER PÅ ["+ other.team_id +":"+other.getId()+"]");
          //fire();
        } else {
          retreat(other);
        }

        rotateTo(other.position);
        //wander();
      } else {
        wander();
      }
    }
    
    //*******************************************************  
    // Tanken meddelas om den har kommit hem.
    public void message_arrivedAtHomebase() {
      //println("*** Team"+this.team_id+".Tank["+ this.getId() + "].message_isAtHomebase()");
      println("! Hemma!!! Team"+this.team_id+".Tank["+ this.getId() + "]");
    }

    //*******************************************************
    // används inte.
    public void readyAfterHit() {
      super.readyAfterHit();
      println("*** Team"+this.team_id+".Tank["+ this.getId() + "].readyAfterHit()");

      //moveTo(grid.getRandomNodePosition());
      wander();
    }

    //*******************************************************
    public void arrivedRotation() {
      super.arrivedRotation();
      println("*** Team"+this.team_id+".Tank["+ this.getId() + "].arrivedRotation()");
      //moveTo(new PVector(int(random(width)),int(random(height))));
      arrived();
    }

    //*******************************************************
    public void arrived() {
      super.arrived();
      previousNode = grid.getNearestNode(this.position);
      println("*** Team"+this.team_id+".Tank["+ this.getId() + "].arrived()");

      //moveTo(new PVector(int(random(width)),int(random(height))));
      //moveTo(grid.getRandomNodePosition());
      wander();
    }

    //*******************************************************
    public void updateLogic() {
      super.updateLogic();
      if (!started) {
        for (int i = 0; i < 2; i++) {
          for (int j = 0; j < 6; j++) {
            this.homeBase.add(grid.getNode(i,j));
          }
        }
        started = true;
        //wander();
        nodeStack.push(grid.getNearestNode(this.position));
      }

      if (!this.userControlled) {
        if (!this.chill_state) {  
          //moveForward_state();
          if (this.stop_state) {
            //rotateTo()
              wander();
          }

          if (this.idle_state) {
            wander();
          }
        }
      }
    }
  }
}
class Team2 extends Team {

  Team2(int team_id, int tank_size, int c, 
    PVector tank0_startpos, int tank0_id, CannonBall ball0, 
    PVector tank1_startpos, int tank1_id, CannonBall ball1, 
    PVector tank2_startpos, int tank2_id, CannonBall ball2) {
    super(team_id, tank_size, c, tank0_startpos, tank0_id, ball0, tank1_startpos, tank1_id, ball1, tank2_startpos, tank2_id, ball2);  

    tanks[0] = new Tank(tank0_id, this, this.tank0_startpos, this.tank_size, ball0);
    tanks[1] = new Tank(tank1_id, this, this.tank1_startpos, this.tank_size, ball1);
    tanks[2] = new Tank(tank2_id, this, this.tank2_startpos, this.tank_size, ball2);

    //this.homebase_x = width - 151;
    //this.homebase_y = height - 351;
  }

  public void updateLogic() {
    //for (int i = 0; i < tanks.length; i++) {
    //  tanks[i].updateLogic();
    //}
  }

  //==================================================
  public class Tank1 extends Tank {

    boolean started;
    Sensor locator;
    Sensor us_front; //ultrasonic_sensor front

    Tank1(int id, Team team, PVector startpos, float diameter, CannonBall ball) {
      super(id, team, startpos, diameter, ball);

      us_front = getSensor("ULTRASONIC_FRONT");
      addSensor(us_front);

      started = false;
    }

    public void initialize() {
    }

    // Tanken meddelas om kollision med tree.
    public void message_collision(Tree other) {
      println("*** Team"+this.team_id+".Tank["+ this.getId() + "].collision(Tree)");

      chooseAction();
    }

    // Tanken meddelas om kollision med tanken.
    public void message_collision(Tank other) {
      println("*** Team"+this.team_id+".Tank["+ this.getId() + "].collision(Tank)");

      chooseAction();
    }

    public void arrived() {
      super.arrived(); // Tank
      println("*** Team"+this.team_id+".Tank["+ this.getId() + "].arrived()");

      chooseAction();
    }

    public void arrivedRotation() {
      super.arrivedRotation();

      println("*** Team"+this.team_id+".Tank["+ this.getId() + "].arrivedRotation()");
      //moveTo(new PVector(int(random(width)),int(random(height))));
      //moveTo(grid.getRandomNodePosition()); // Slumpmässigt mål.
      moveForward_state(); // Tank
    }

    public void chooseAction() {
      //moveTo(grid.getRandomNodePosition());
      println("*** Team"+this.team_id+".Tank["+ this.getId() + "].chooseAction()");
      //resetTargetStates(); // Tank
      //resetAllMovingStates(); // Tank

      float r = random(1, 360);
      rotateTo(radians(r));
    }

    public void readSensorDistance() {
      SensorReading sr = readSensor_distance(us_front);
      //println("1sr.distance(): "+ sr.distance());
      if ((sr.distance() < this.radius) && this.isMoving) {
        if (!this.stop_state) {
          println("Team"+this.team_id+".Tank["+ this.getId() + "] Har registrerat ett hinder. (Tank.readSensorDistance())");
          //stopMoving();
          //stopTurning_state()
          //this.stop_state = true;
          stopMoving_state(); //Tank
          //chooseAction();
        }
      }
    }

    public void updateLogic() {
      //super.updateLogic();


      // Avoid contact with other objects and tanks.
      float threshold = .1f;
      //println("========================================");
      //println("Team"+this.team_id+".Tank["+ this.getId() + "] : " + us_front.readValue(0));
      //if (us_front.readValue(0) < threshold) {
      //  println("*** Team"+this.team_id+".Tank["+ this.getId() + "]: (us_front.readValue(0) < threshold)");
      //}

      // println("Team"+this.team_id+".Tank["+ this.getId() + "] : " + us_front.readValue1());



      if (!started) {
        started = true;
        initialize();

        moveForward_state();
        //moveForward();
      }

      if (!this.userControlled) {
        readSensorDistance();

        //moveForward_state();
        if (this.idle_state) {
          //rotateTo()
          chooseAction();
        }
      }
    }
  }

  //==================================================
  public class Tank2 extends Tank {

    boolean started;

    //*******************************************************
    Tank2(int id, Team team, PVector startpos, float diameter, CannonBall ball) {
      super(id, team, startpos, diameter, ball);

      this.started = false; 

      //this.isMoving = true;
      //moveTo(grid.getRandomNodePosition());
    }

    //*******************************************************
    // Reterera, fly!
    public void retreat() {
      println("*** Team"+this.team_id+".Tank["+ this.getId() + "].retreat()");
      moveTo(grid.getRandomNodePosition()); // Slumpmässigt mål.
    }

    //*******************************************************
    // Reterera i motsatt riktning (ej implementerad!)
    public void retreat(Tank other) {
      //println("*** Team"+this.team_id+".Tank["+ this.getId() + "].retreat()");
      //moveTo(grid.getRandomNodePosition());
      retreat();
    }

    //*******************************************************
    // Fortsätt att vandra runt.
    public void wander() {
      println("*** Team"+this.team_id+".Tank["+ this.getId() + "].wander()");
      //rotateTo(grid.getRandomNodePosition());  // Rotera mot ett slumpmässigt mål.
      moveTo(grid.getRandomNodePosition()); // Slumpmässigt mål.
    }


    //*******************************************************
    // Tanken meddelas om kollision med trädet.
    public void message_collision(Tree other) {
      println("*** Team"+this.team_id+".Tank["+ this.getId() + "].collision(Tree)");
      wander();
    }

    //*******************************************************
    // Tanken meddelas om kollision med tanken.
    public void message_collision(Tank other) {
      println("*** Team"+this.team_id+".Tank["+ this.getId() + "].collision(Tank)");

      //moveTo(new PVector(int(random(width)),int(random(height))));
      //println("this.getName());" + this.getName()+ ", this.team_id: "+ this.team_id);
      //println("other.getName());" + other.getName()+ ", other.team_id: "+ other.team_id);

      if ((other.getName() == "tank") && (other.team_id != this.team_id)) {
        if (this.hasShot && (!other.isDestroyed)) {
          println("["+this.team_id+":"+ this.getId() + "] SKJUTER PÅ ["+ other.team_id +":"+other.getId()+"]");
          fire();
        } else {
          retreat(other);
        }

        rotateTo(other.position);
        //wander();
      } else {
        wander();
      }
    }
    
    //*******************************************************  
    // Tanken meddelas om den har kommit hem.
    public void message_arrivedAtHomebase() {
      //println("*** Team"+this.team_id+".Tank["+ this.getId() + "].message_isAtHomebase()");
      println("! Hemma!!! Team"+this.team_id+".Tank["+ this.getId() + "]");
    }

    //*******************************************************
    // används inte.
    public void readyAfterHit() {
      super.readyAfterHit();
      println("*** Team"+this.team_id+".Tank["+ this.getId() + "].readyAfterHit()");

      //moveTo(grid.getRandomNodePosition());
      wander();
    }

    //*******************************************************
    public void arrivedRotation() {
      super.arrivedRotation();
      println("*** Team"+this.team_id+".Tank["+ this.getId() + "].arrivedRotation()");
      //moveTo(new PVector(int(random(width)),int(random(height))));
      arrived();
    }

    //*******************************************************
    public void arrived() {
      super.arrived();
      println("*** Team"+this.team_id+".Tank["+ this.getId() + "].arrived()");

      //moveTo(new PVector(int(random(width)),int(random(height))));
      //moveTo(grid.getRandomNodePosition());
      wander();
    }

    //*******************************************************
    public void updateLogic() {
      super.updateLogic();

      if (!started) {
        started = true;
        moveTo(grid.getRandomNodePosition());
      }

      if (!this.userControlled) {

        //moveForward_state();
        if (this.stop_state) {
          //rotateTo()
          wander();
        }

        if (this.idle_state) {
          wander();
        }
        
        
      }
    }
  }

  //==================================================
  public class Tank3 extends Tank {

    PVector cr = new PVector();
    float wandertheta;
    float maxforce;

    Tank3(int id, Team team, PVector startpos, float diameter, CannonBall ball) {
      super(id, team, startpos, diameter, ball);

      this.wandertheta = 0;
      this.maxforce = 0.05f;
      this.stop_state = false;
    }

    //--------------------
    // A method that calculates and applies a steering force towards a target
    // STEER = DESIRED MINUS VELOCITY
    public void seek(PVector target) {
      PVector desired = PVector.sub(target, position);  // A vector pointing from the position to the target

      // Normalize desired and scale to maximum speed
      desired.normalize();
      desired.mult(maxspeed);


      // Steering = Desired minus Velocity
      PVector steer = PVector.sub(desired, velocity);

      steer.limit(maxforce);  // Limit to maximum steering force
      //println("steer: " + steer.getHeading());

      if (steer.heading() < 0) {
        this.turning_right_state = false;
        this.turning_left_state = true;
      } else
        if (steer.heading() > 0) {
          this.turning_right_state = true;
          this.turning_left_state = false;
        }

      //applyForce(steer);
    }

    public void wander() {
      float wanderR = 25;         // Radius for our "wander circle"
      float wanderD = 200;//80         // Distance for our "wander circle"
      float change = 0.3f;
      wandertheta += random(-change, change);     // Randomly change wander theta

      // Now we have to calculate the new position to steer towards on the wander circle
      PVector circlepos = new PVector();
      circlepos.set(velocity);    // Start with velocity
      circlepos.normalize();            // Normalize to get heading
      circlepos.mult(wanderD);          // Multiply by distance
      circlepos.add(position);               // Make it relative to boid's position

      float h = velocity.heading();        // We need to know the heading to offset wandertheta
      //float h = this.heading;        // We need to know the heading to offset wandertheta

      PVector circleOffSet = new PVector(wanderR*cos(wandertheta+h), wanderR*sin(wandertheta+h));
      PVector target = PVector.add(circlepos, circleOffSet);

      seek(target);

      // Render wandering circle, etc.
      if (debugOn) drawWanderStuff(position, circlepos, target, wanderR);
    }

    // A method just to draw the circle associated with wandering
    public void drawWanderStuff(PVector position, PVector circle, PVector target, float rad) {
      stroke(0);
      noFill();
      ellipseMode(CENTER);
      ellipse(circle.x, circle.y, rad*2, rad*2);

      ellipse(circle.x, circle.y, 10, 10);
      //ellipse(position.x,position.y,10,10);

      ellipse(target.x, target.y, 4, 4);
      line(position.x, position.y, circle.x, circle.y);
      line(circle.x, circle.y, target.x, target.y);
    }
    //--------------------

    public void checkFront_sensor() {
    }

    public void checkEnvironment_sensor() {
      float tempx = 0;
      PVector w = new PVector(0, 0);
    }

    public void updateLogic() {
      super.updateLogic();

      if (!this.userControlled) {
        checkEnvironment_sensor();


        if (!this.stop_state) {
          moveForward_state();
          wander();

          //println("heading1: " + this.heading);
          //println("velocity1: " + this.velocity); 
          //println("velocityheading1: " + this.velocity.getHeading()); 
          //this.heading = this.velocity.getHeading();
        }
      }
    }
  }
}
/**
  * A ticker class to manage animation timing.
  */
public class Timer{

  private int lastTime;
  private float deltaTime;
  private boolean isPaused;
  private float totalTime;
  private boolean countingUp; 
  
  public Timer(){
    reset();
  }
  
  public void setDirection(String d){
    if (d == "down") {
      countingUp = false;
    }
  }
  
  public void reset(){
    deltaTime = 0f;
    lastTime = -1;
    isPaused = false;
    totalTime = 0f;
    countingUp = true;
  }
  
  //
  public void pause(){
    isPaused = true;
  }
  
  public void resume(){
    deltaTime = 0f;
    lastTime = -1;
    isPaused = false;
  }
  
  public void setTime(int min, int sec){    
    totalTime = min * 600 + sec;
  }
  
  /*
      Format: 5.5 = 5 minutes 30 seconds
  */
  public void setTime(float minutes){
    int int_min = (int)minutes;
    int sec = (int)((minutes - (float)int_min) * 60);
    setTime( int_min, sec);
  }
  
  public float getTotalTime(){
    return totalTime;
  }
  
  /*
  */
  public float getDeltaSec(){
    if(isPaused){
      return 0;
    }
    return deltaTime;
  }
  
  /*
  * Calculates how many seconds passed since the last call to this method.
  *
  */
  public void tick(){
    if(lastTime == -1){
      lastTime = millis();
    }
    
    int delta = millis() - lastTime;
    lastTime = millis();
    deltaTime = delta/1000f;
    
    if(countingUp){
      totalTime += deltaTime;
    }
    else{
      totalTime -= deltaTime;
    }
  }
}
class Tree extends Sprite {
  
  //PVector position;
  //String name;
   
  PImage img;
  //PVector hitArea;
  //float diameter, radius, m;
  //float m;
  
  //**************************************************
  Tree(int posx, int posy) {
    this.img = loadImage("tree01_v2.png");
    this.position = new PVector(posx, posy);
    //this.hitArea = new PVector(posx, posy); // Kanske inte kommer att användas.
    this.diameter = this.img.width/2;
    this.radius = diameter/2;
    //this.m = radius*.1;
    
    this.name = "tree";
  }

  //**************************************************
  public void checkCollision(Tank other) {
    

    // Get distances between the balls components
    PVector distanceVect = PVector.sub(other.position, position);

    // Calculate magnitude of the vector separating the balls
    float distanceVectMag = distanceVect.mag();

    // Minimum distance before they are touching
    float minDistance = radius + other.radius;

    if (distanceVectMag < minDistance) {
      println("! collision med en tank [Tree]");
      
    }
    
  }

  //**************************************************  
  public void display() {
    pushMatrix();
    translate(this.position.x, this.position.y);
    
      fill(204, 102, 0, 100);
      int diameter = this.img.width/2;
      //ellipse(this.position.x, this.position.y, diameter, diameter);
      ellipse(0, 0, diameter, diameter);
      //image(img, this.position.x, this.position.y);
      image(img, 0, 0);
     
      if(debugOn){
        noFill();
        stroke(255, 0, 0);
        ellipse(0, 0, this.diameter(), this.diameter());
      }
      popMatrix();
      
  }
}
class Turret {
  PImage img;
  float rotation_speed;
  float cannon_length;
  
  PVector position;
  //PVector velocity;
  //PVector acceleration;
  // Variable for heading!
  float heading;
  
  
  Turret(float cannon_length) {
    //this.img = loadImage("gunTurret2.png");
    this.position = new PVector(0.0f, 0.0f);
    
    this.cannon_length = cannon_length;
    this.heading = 0.0f;
    this.rotation_speed = radians(1);
  }
  
  public void turnLeft() {
    this.heading -= this.rotation_speed;
  }
  
  public void turnRight() {
    this.heading += this.rotation_speed;
  }
  
  public void drawTurret(){
    strokeWeight(1);
    //fill(204, 50, 50);
    ellipse(0, 0, 25, 25);
    strokeWeight(3);
    line(0, 0, this.cannon_length, 0);
  }
  
  public void fire() {
    
  }
  
  public void display() {  
    this.position.x = cos(this.heading);
    this.position.y = sin(this.heading);
    
    rotate(this.heading);
    //image(img, 20, 0);
    drawTurret();
    
  }
}
// Initiera användargränssnittet.
// Används inte.
public void setGUI() {
  println("*** setGUI()- Användargränsnittet skapas.");

}
//**************************************************
// Gör så att allt i användargränssnittet (GUI) visas.
public void showGUI() {
  //println("*** showGUI()");

  textSize(14);
  fill(30);
  text("Team1: "+teams[0].numberOfHits, 10, 20);
  text("Team2: "+teams[1].numberOfHits, width-100, 20);
  textSize(24);
  text(remainingTime, width/2, 25);
  textSize(14);
  
  
  if (userControl) {
    // Draw an ellipse at the mouse position
    PVector mouse = new PVector(mouseX, mouseY);
    fill(200);
    stroke(0);
    strokeWeight(2);
    ellipse(mouse.x, mouse.y, 48, 48);
    //grid.displayNearestNode(mouseX, mouseY);
  }


  if (debugOn) {
    // Visa framerate.
    fill(30);
    text("FPS:"+ floor(frameRate), 10, height-10);

    // Visa grid.
    fill(205);
    gridDisplay();

    // Visa musposition och den närmaste noden.
    fill(255, 92, 92);
    ellipse(mouseX, mouseY, 5, 5);
    grid.displayNearestNode(mouseX, mouseY);
  }
  
  if (pause) {
    textSize(36);
    fill(30);
    text("Paused!", width/2-100, height/2);
  }
  
  if (gameOver) {
    textSize(36);
    fill(30);
    text("Game Over!", width/2-100, height/2);
  }
}
//**************************************************
// Gör så att textfälten visas och uppdateras. 
// Används inte.
public void showOutput() {
}

public void displayTrees() {
  for (int i = 0; i<allTrees.length; i++) {
    allTrees[i].display();
  }
}


public void gridDisplay() {
  strokeWeight(0.3f);

  grid.display();
}

public void updateTanksDisplay() {
  //for (int i = 0; i < allTanks.length; i++) {
  //  allTanks[i].display();
  //}
  for (Tank tank : allTanks) {
    tank.display();
  }
}

public void updateShotsDisplay() {
  for (int i = 0; i < allShots.length; i++) {
    allShots[i].display();
  }
}
public void checkForInput() {
  if (userControl) {

    if (alt_key) {
      //println("alt_key: " + alt_key);
      if (left) {
        allTanks[tankInFocus].turnTurretLeft_state();
      } else if (right) {
        allTanks[tankInFocus].turnTurretRight_state();
      }
    } else 
    if (!alt_key) {

      if (left) {
        allTanks[tankInFocus].turnLeft_state();
      } else if (right) {
        allTanks[tankInFocus].turnRight_state();
      }

      if (up) {
        allTanks[tankInFocus].moveForward_state();
      } else if (down) {
        allTanks[tankInFocus].moveBackward_state();
      }

      if (!(up || down)) {
        //allTanks[tankInFocus].deaccelarate();
        //allTanks[tankInFocus].stopMoving_state();
      }
      if (!(right || left)) {
        //allTanks[tankInFocus].deaccelarate();
        //allTanks[tankInFocus].stopTurning_state();
      }
    }

    if (!(alt_key && (left || right))) {
      //allTanks[tankInFocus].stopTurretTurning_state();
    }

    if (mouse_pressed) {
      println("if (mouse_pressed)");
      //allTanks[tankInFocus].spin(3);
      int mx = mouseX;
      int my = mouseY;

      setTargetPosition(new PVector(mx, my));

      mouse_pressed = false;
    }
  }
}
public void keyPressed() {
  if (userControl) {

    if (key == CODED) {
      switch(keyCode) {
      case LEFT:
        //myTank1_snd.engineStart();
        left = true;
        break;
      case RIGHT:
        //myTank_snd.engineStart();
        right = true;
        break;
      case UP:
        //myTank_snd.engineStart();
        up = true;
        break;
      case DOWN:
        //myTank_snd.engineStart();
        down = true;
        break;
      case ALT:
        // turret.
        alt_key = true;
        break;
      }
    }
    if (key == ' ') {
      //myAudio.shot();
      //myAudio.blast();
      //myTank1.fire(); 
      println("keyPressed SPACE");
      allTanks[tankInFocus].fire();
    }
  }

  if (key == 'c') {
    userControl = !userControl;
    
//    allTanks[tankInFocus].stopMoving_state();
//    allTanks[tankInFocus].stopTurning_state();
//    allTanks[tankInFocus].stopTurretTurning_state();
    
    if (!userControl) {
      allTanks[tankInFocus].releaseControl();
        
    } else {
      allTanks[tankInFocus].takeControl();
    }
  }
  
  if (key == 'p') {
    pause = !pause;
    if (pause) {
      timer.pause();
    } else {
      timer.resume();
    }
  }

  if (key == 'd') {
    debugOn = !debugOn;
  }
}

public void keyReleased() {
  if (userControl) {

    if (key == CODED) {
      switch(keyCode) {
      case LEFT:
        //myTank_snd.engineStop();
        left = false;
        allTanks[tankInFocus].stopTurning_state();
        break;
      case RIGHT:
        //myTank_snd.engineStop();
        right = false;
        allTanks[tankInFocus].stopTurning_state();
        break;
      case UP:
        //myTank_snd.engineStop();
        up = false;
        allTanks[tankInFocus].stopMoving_state();
        break;
      case DOWN:
        //myTank_snd.engineStop();
        down = false;
        allTanks[tankInFocus].stopMoving_state();
        break;
      case ALT:
        // turret.
        alt_key = false;
        allTanks[tankInFocus].stopTurretTurning_state();
      }
    }
  }
}

public void keyTyped() {

  if (userControl) {
    switch(key) {
    case '1':
      allTanks[tankInFocus].releaseControl();
      tankInFocus = 1;
      allTanks[tankInFocus].takeControl();
      break;
    case '2':
      allTanks[tankInFocus].releaseControl();
      tankInFocus = 2;
      allTanks[tankInFocus].takeControl();
      break;
    case '3':
      allTanks[tankInFocus].releaseControl();
      tankInFocus = 3;
      allTanks[tankInFocus].takeControl();
      break;
    case '4':
      allTanks[tankInFocus].releaseControl();
      tankInFocus = 4;
      allTanks[tankInFocus].takeControl();
      break;
    case '5':
      allTanks[tankInFocus].releaseControl();
      tankInFocus = 5;
      allTanks[tankInFocus].takeControl();
      break;
    case '0':
      allTanks[tankInFocus].releaseControl();
      tankInFocus = 0;
      allTanks[tankInFocus].takeControl();
      break;
    }
  }
}
// Mouse functions



// Mousebuttons
public void mousePressed() {
  println("---------------------------------------------------------");
  println("*** mousePressed() - Musknappen har tryckts ned.");
  
  mouse_pressed = true;
  
}

// call to Team updateLogic()
public void updateTeamsLogic() {
  teams[0].updateLogic();
  teams[1].updateLogic();
}

// call to Tank updateLogic()
public void updateTanksLogic() {
  
  for (Tank tank : allTanks) {
    if (tank.isReady) {
      tank.updateLogic();
    }
  }
  
  //for (int i = 0; i < tanks.length; i++) {
  //  this.tanks[i].updateLogic();
  //}
}

// call to Tank update()
public void updateTanks() {
  
  for (Tank tank : allTanks) {
    //if (tank.health > 0)
    tank.update();
  }
  
  //for (int i = 0; i < tanks.length; i++) {
  //  this.tanks[i].updateLogic();
  //}
}

public void updateShots() {
  for (int i = 0; i < allShots.length; i++) {
    if ((allShots[i].passedTime > wait) && (!allTanks[i].hasShot)) {
      allShots[i].resetTimer();
      allTanks[i].loadShot();
    }
    allShots[i].update();
  }
}

public void checkForCollisionsShots() {
  for (int i = 0; i < allShots.length; i++) {
    if (allShots[i].isInMotion) {
      for (int j = 0; j<allTrees.length; j++) {
        allShots[i].checkCollision(allTrees[j]);
      }
     
      for (int j = 0; j < allTanks.length; j++) {
        if (j != allTanks[i].getId()) {
          allShots[i].checkCollision(allTanks[j]);
        }
      }
    }
  }
}

public void checkForCollisionsTanks() {
  // Check for collisions with Canvas Boundaries
  for (int i = 0; i < allTanks.length; i++) {
    allTanks[i].checkEnvironment();
    
    // Check for collisions with "no Smart Objects", Obstacles (trees, etc.)
    for (int j = 0; j < allTrees.length; j++) {
      allTanks[i].checkCollision(allTrees[j]);
    }
    
    // Check for collisions with "Smart Objects", other Tanks.
    for (int j = 0; j < allTanks.length; j++) {
      //if ((allTanks[i].getId() != j) && (allTanks[i].health > 0)) {
      if (allTanks[i].getId() != j) {
        allTanks[i].checkCollision(allTanks[j]);
      }
    }
  }
}

public void loadShots() {
  for (int i = 0; i < allTanks.length; i++) {
    allTanks[i].loadShot();
  }
}

//void shoot(Tank id, PVector pvec) {
public void shoot(int id) {
  println("App.shoot()");
 // println(id +": "+ pvec);
  //allShots.get(id).setStartPosition(pvec);
  
  //myAudio.blast();
  
  allShots[id].isInMotion = true;
  allShots[id].startTimer();
}

public void setTargetPosition(PVector pvec) {
  PVector nodevec = grid.getNearestNodePosition(pvec);
  //allTanks[tankInFocus].moveTo(pvec);
  allTanks[tankInFocus].moveTo(nodevec);
}

//******************************************************

/**
 * Find the point of intersection between two lines.
 * This method uses PVector objects to represent the line end points.
 * @param v0 start of line 1
 * @param v1 end of line 1
 * @param v2 start of line 2
 * @param v3 end of line 2
 * @return a PVector object holding the intersection coordinates else null if no intersection 
 */
public PVector line_line_p(PVector v0, PVector v1, PVector v2, PVector v3){
  final double ACCY   = 1e-30f;
  PVector intercept = null;

  double f1 = (v1.x - v0.x);
  double g1 = (v1.y - v0.y);
  double f2 = (v3.x - v2.x);
  double g2 = (v3.y - v2.y);
  double f1g2 = f1 * g2;
  double f2g1 = f2 * g1;
  double det = f2g1 - f1g2;

  if(abs((float)det) > (float)ACCY){
    double s = (f2*(v2.y - v0.y) - g2*(v2.x - v0.x))/ det;
    double t = (f1*(v2.y - v0.y) - g1*(v2.x - v0.x))/ det;
    if(s >= 0 && s <= 1 && t >= 0 && t <= 1)
      intercept = new PVector((float)(v0.x + f1 * s), (float)(v0.y + g1 * s));
  }
  return intercept;
}
  
//******************************************************
//Används inte.
/**
   * Put angle in degrees into [0, 360) range
   */
//  public static float fixAngle(float angle) {
public float fixAngle(float angle) {
    while (angle < 0f)
      angle += 360f;
    while (angle >= 360f)
      angle -= 360f;
    return angle;
}

//Används inte.
//public static float relativeAngle(float delta){
public float relativeAngle(float delta){
    while (delta < 180f)
      delta += 360f;
    while (delta >= 180f)
      delta -= 360f;
    return delta;
}
  public void settings() {  size(800, 800); }
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "tanks_190324" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
