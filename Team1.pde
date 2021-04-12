import java.util.HashSet;
import java.util.Set;
import java.util.*;
class Team1 extends Team {

  Team1(int team_id, int tank_size, color c, 
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
    Node previousNode;
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
          for(Node node : nodeStack){
          System.out.println( node.getRow()+" : "+node.getCol());
        }

        Node currentNode = nodeStack.pop();
        moveTo(currentNode.position);

        //om current node inte finns i traversed node, lägg till den
        if(!traversedNodes.contains(currentNode)){
          traversedNodes.add(currentNode);
          
          //För varje närliggande nod pusha den noden till stacken.
          for(Node node : grid.getNearestNodes(currentNode)){
            if(!traversedNodes.contains(node)){
                nodeStack.push(node);
            }
          }
        }

      } else if (!this.total_path.isEmpty()){
        //moveTo(this.total_path.pop().position);
        moveTo(this.total_path.get(this.total_path.size()-1).position);
        total_path.remove(this.total_path.size()-1);
      } else {
        this.searching = true;
      }
    }



/*
    public void depthFirstSearch(Node currentNode){
      Stack<Node> nodeStack = new Stack<Node>();
      nodeStack.push(currentNode);
      System.out.println("PUSH PUSH");

      while(!nodeStack.empty()){
        for(Node node : nodeStack){
          System.out.println(node.getCol() +" : "+ node.getRow());
        }
        currentNode = nodeStack.pop();
        moveTo(currentNode.position);

        if(!traversedNodes.contains(currentNode)){
          traversedNodes.add(currentNode);

          for(Node node : grid.getNearestNodes(currentNode)){
              nodeStack.push(node);
          }
        }
      }
    }
    */

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
        started = true;
        //wander();
        nodeStack.push(grid.getNearestNode(this.position));
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
}
