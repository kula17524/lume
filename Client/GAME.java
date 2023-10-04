//@2023 ohkura china
package main;

import com.jme3.app.SimpleApplication;
import com.jme3.app.*;
import com.jme3.audio.AudioData.DataType;
import com.jme3.audio.AudioNode;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.font.LineWrapMode;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.BloomFilter;
import com.jme3.scene.CameraNode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.CameraControl.ControlDirection;
import com.jme3.scene.control.LightControl;
import com.jme3.scene.shape.Sphere;
import com.jme3.system.AppSettings;
import java.io.*;
import java.net.*;
import javax.swing.*;

// main source code
public class GAME extends SimpleApplication implements ActionListener {

    private BulletAppState bulletAppState;
    private CharacterControl myPhysicsCharacter;
    private Node youCharacterNode = new Node("youcharacter");
    final private Vector3f walkDirection = new Vector3f(0,0,0);
    final private Vector3f viewDirection = new Vector3f(0,0,0);
    private boolean leftStrafe = false, rightStrafe = false, forward = false, backward = false, leftRotate = false, rightRotate = false;
    
    // shooting
    private Node shootables = new Node("Shootables");
  
    // sound
    private AudioNode audio_cannon;
    private AudioNode audio_hit;
    
    // network
    private static String serverIP;
    private static String myName;
    PrintWriter out;
    private Vector3f myPos, youPos;
    private int myShot = 0;
    private int youShot = 0;
    private int myNumber;
    
    // text
    BitmapFont guiFont;
    BitmapText myScore;
    BitmapText youScore;
    BitmapText resultTx;
    BitmapText resultTx2;
    
    // result
    private boolean iWin = false;
    private boolean iLose = false;
    private boolean draw = false;
    private boolean canPlay = true;
  
    public GAME() {
        // input mydata
        myName = JOptionPane.showInputDialog(null,"名前を入力してください","名前の入力",JOptionPane.QUESTION_MESSAGE);
        if(myName.equals("")){
            myName = "No name";
	}
        serverIP = JOptionPane.showInputDialog(null,"IPアドレスを入力してください","IPアドレスの入力",JOptionPane.QUESTION_MESSAGE);
        if(serverIP.equals("")){
            serverIP = "localhost";
        }
        
        // connect to server
	Socket socket = null;
	try {
            socket = new Socket(serverIP, 10000);
	} catch (UnknownHostException e) {
            System.err.println("ホストの IP アドレスが判定できません: " + e);
	} catch (IOException e) {
            System.err.println("エラーが発生しました: " + e);
	}
		
	MesgRecvThread mrt = new MesgRecvThread(socket, myName);
	mrt.start();
    }
    
    // connect server
    public class MesgRecvThread extends Thread {
	Socket socket;
	String myName;
		
	public MesgRecvThread(Socket s, String n){
            socket = s;
            myName = n;
	}
        
	public void run() {
            try {
		InputStreamReader sisr = new InputStreamReader(socket.getInputStream());
		BufferedReader br = new BufferedReader(sisr);
		out = new PrintWriter(socket.getOutputStream(), true);
		out.println(myName);
		String myNumberStr = br.readLine();
		int myNumberInt = Integer.parseInt(myNumberStr);
                myNumber = myNumberInt % 2;
		if (myNumber == 0) {
                    myPos = new Vector3f(90, -90, -90);
                    youPos = new Vector3f(200, 200, 200);
		} else {
                    myPos = new Vector3f(-90, -90, 90);
                    youPos = new Vector3f(200, 200, 200);
		}
                
                // initialize shot count
                myShot = 0;
                youShot = 0;

                // when something changed
		while(true) {
                    String inputLine = br.readLine();
                    if (inputLine != null) {
			System.out.println(inputLine);
			String[] inputTokens = inputLine.split(" ");
			String cmd = inputTokens[0];

                        // if player move:
			if(cmd.equals("MOVE")){
                            int me_or_you = Integer.parseInt(inputTokens[1]);
                            if(me_or_you != myNumber){
				youPos.x = Float.parseFloat(inputTokens[2]);
                                youPos.y = Float.parseFloat(inputTokens[3]) - 1.7f;
                                youPos.z = Float.parseFloat(inputTokens[4]);
                            }
                        }
                        // if player shot
                        if(cmd.equals("SHOT")){
                            int me_or_you = Integer.parseInt(inputTokens[1]);
                            int hit_or_no = Integer.parseInt(inputTokens[2]);
                            if(me_or_you == myNumber){
                                if (hit_or_no == 1) { // i hit
                                    myShot += 1;
                                }
                            }else{
                                if (hit_or_no == 1) { // rival hit
                                    youShot += 1;
                                }
                            }
                        }
                        // gameset
                        if ((myShot == 5) && (youShot == 5)) {
                            draw = true;
                            canPlay = false;
                        } else if (myShot == 5) {
                            iWin = true;
                            canPlay = false;
                        } else if (youShot == 5) {
                            iLose = true;
                            canPlay = false;
                        }
                    }else{
                        break;
                    }
                }
                socket.close();
            } catch (IOException e) {
                System.err.println("Error!: " + e);
            }
        }
    }
    
    public static void main(String[] args) {
        // start app
        GAME app = new GAME();
        AppSettings settings = new AppSettings(true);
        settings.setTitle("Lume.");
        settings.setResolution(1280,720);
        settings.setGammaCorrection(true);
        settings.setSamples(4);
    
        app.setShowSettings(false);
        app.setDisplayFps(false);
        app.setSettings(settings);
        app.start();
    }
  
    // key settings
    private void setUpKeys() {
        inputManager.addMapping("Strafe Left", 
            new KeyTrigger(KeyInput.KEY_A), 
            new KeyTrigger(KeyInput.KEY_LEFT));
        inputManager.addMapping("Strafe Right", 
            new KeyTrigger(KeyInput.KEY_D), 
            new KeyTrigger(KeyInput.KEY_RIGHT));
        inputManager.addMapping("Rotate Left", 
            new KeyTrigger(KeyInput.KEY_Q));
        inputManager.addMapping("Rotate Right", 
            new KeyTrigger(KeyInput.KEY_E));
        inputManager.addMapping("Walk Forward", 
            new KeyTrigger(KeyInput.KEY_W), 
            new KeyTrigger(KeyInput.KEY_UP));
        inputManager.addMapping("Walk Backward", 
            new KeyTrigger(KeyInput.KEY_S),
            new KeyTrigger(KeyInput.KEY_DOWN));
        inputManager.addMapping("Jump", 
                new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("Shoot", 
            new MouseButtonTrigger(MouseInput.BUTTON_LEFT), 
            new KeyTrigger(KeyInput.KEY_RETURN));
        inputManager.addListener(this, "Strafe Left", "Strafe Right");
        inputManager.addListener(this, "Rotate Left", "Rotate Right");
        inputManager.addListener(this, "Walk Forward", "Walk Backward");
        inputManager.addListener(this, "Jump", "Shoot");
    }

    @Override
    public void simpleInitApp() {
      
        // back ground color
        viewPort.setBackgroundColor(new ColorRGBA(0f, 0f, 0f, 1f));
    
        // activate physics
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        rootNode.attachChild(shootables);

        // import stage
        Spatial sceneModel = assetManager.loadModel("resources/Models/stage/stage.scene");
        Quaternion pitch90 = new Quaternion();
        pitch90.fromAngleAxis(FastMath.PI*2, new Vector3f(0,0,1));
        sceneModel.setLocalRotation(pitch90);
        sceneModel.setLocalScale(0.5f);
        sceneModel.setLocalTranslation(0, -100, 0);
        // add physics to stage
        CollisionShape sceneShape =
            CollisionShapeFactory.createMeshShape(sceneModel);
        RigidBodyControl landscape = new RigidBodyControl(sceneShape, 0);
        sceneModel.addControl(landscape);
        Node stageNode = new Node("stage");
        stageNode.attachChild(sceneModel);
        shootables.attachChild(stageNode);
        getPhysicsSpace().add(landscape);
    
        setUpKeys();
    
        // add my character
        myPhysicsCharacter = new CharacterControl(new CapsuleCollisionShape(0.5f, 1.8f), .1f);
        myPhysicsCharacter.setPhysicsLocation(new Vector3f(0, 0.9f, 0));
        myPhysicsCharacter.setJumpSpeed(20f);
        myPhysicsCharacter.setFallSpeed(40f);
        Node myCharacterNode = new Node("mycharacter");
        Node myModelNode = new Node("myModel");
        Spatial myModel = assetManager.loadModel("resources/Models/character/chara.scene");
        myModel.scale(0.1f);
        // material
        Material myModel_mat = new Material(assetManager,"Common/MatDefs/Light/Lighting.j3md"); 
        myModel_mat.setBoolean("UseMaterialColors",true);
        myModel_mat.setFloat("Shininess", 1f);
        myModel_mat.setColor("Specular",ColorRGBA.Green);
        myModel_mat.setColor("Ambient", ColorRGBA.Black);
        myModel_mat.setColor("Diffuse", ColorRGBA.Black);
        myModel.setMaterial(myModel_mat);
        // shining
        Material my_mat = new Material(getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        my_mat.setColor("Color", ColorRGBA.Green);
        my_mat.setColor("GlowColor", ColorRGBA.White);
        FilterPostProcessor fpp=new FilterPostProcessor(assetManager);
        BloomFilter bloom= new BloomFilter(BloomFilter.GlowMode.Objects);
        bloom.setBloomIntensity(5);
        bloom.setExposurePower(10);
        bloom.setBlurScale(3);
        fpp.addFilter(bloom);
        viewPort.addProcessor(fpp);
        Sphere sphere = new Sphere(10, 10, 0.4f, true, false);
        Geometry myBall = new Geometry("lightball", sphere);
        myBall.setMaterial(my_mat);
        myBall.setLocalTranslation(0, 0.4f, 0);
        myCharacterNode.attachChild(myBall);
        myCharacterNode.addControl(myPhysicsCharacter);
        PointLight myLight = new PointLight();
        myLight.setRadius(20f);
        myLight.setPosition(new Vector3f(0, 0.4f, 0));
        myLight.setColor(ColorRGBA.White.mult(5.5f)); 
        rootNode.addLight(myLight);
        LightControl myLightControl = new LightControl(myLight);
        myModel.addControl(myLightControl);
        // attach screen
        getPhysicsSpace().add(myPhysicsCharacter);
        shootables.attachChild(myCharacterNode);
        myCharacterNode.attachChild(myModelNode);
        myModelNode.attachChild(myModel);
    
        // set forward camera that follows my character
        CameraNode camNode = new CameraNode("CamNode", cam);
        Quaternion roll5_x = new Quaternion();
        roll5_x.fromAngleAxis( FastMath.PI * 5 / 180, new Vector3f(-1,1,0) );
        camNode.setControlDir(ControlDirection.SpatialToCamera);
        camNode.setLocalTranslation(new Vector3f(0, 0.7f, 1f));
        myCharacterNode.attachChild(camNode);
        myCharacterNode.setLocalTranslation(myPos);
        myModelNode.move(0, -1.7f, 0);
    
        //disable the default 1st-person flyCam
        flyCam.setEnabled(false);
        
        // set rival's character
        Spatial youModel = assetManager.loadModel("resources/Models/character/chara.scene");
        youModel.scale(0.1f);
        youModel.setLocalRotation(pitch90);
        youModel.setLocalTranslation(youPos);
        youCharacterNode.setLocalTranslation(youPos);
        CapsuleCollisionShape youShape = new CapsuleCollisionShape(2f, 2f);
        RigidBodyControl youRigid = new RigidBodyControl(youShape, 0);
        youModel.addControl(youRigid);
        shootables.attachChild(youCharacterNode);
        // material
        Material youModel_mat = new Material(assetManager,"Common/MatDefs/Light/Lighting.j3md"); 
        youModel_mat.setBoolean("UseMaterialColors",true);
        youModel_mat.setFloat("Shininess", 5f);
        youModel_mat.setColor("Specular",ColorRGBA.Green);
        youModel_mat.setColor("Ambient", ColorRGBA.Black);
        youModel_mat.setColor("Diffuse", ColorRGBA.Black);
        youModel.setMaterial(youModel_mat);
        // shining
        PointLight youLight = new PointLight();
        youLight.setRadius(20f);
        youLight.setPosition(new Vector3f(youPos.x, youPos.y + 0.4f, youPos.z));
        youLight.setColor(ColorRGBA.White.mult(5.5f)); 
        rootNode.addLight(youLight);
        LightControl youLightControl = new LightControl(youLight);
        youModel.addControl(youLightControl);
        // attach screen
        getPhysicsSpace().add(youRigid);
        youCharacterNode.attachChild(youModel);
        youCharacterNode.move(0, -1.7f, 0);
        shootables.attachChild(youCharacterNode);
        youCharacterNode.setLocalTranslation(youPos);
        
        // score
        guiFont = assetManager.loadFont("resources/Interface/Fonts/gui.fnt");
        myScore = new BitmapText(guiFont, false);
        String txt = "You: " + myShot + " / 5";
        myScore.setText(txt);
        myScore.setSize(44);
        myScore.setLocalTranslation(10, settings.getHeight()-10,0);
        guiNode.attachChild(myScore);
        
        youScore = new BitmapText(guiFont);
        String rt = "Rival: " + youShot + " / 5";
        youScore.setText(rt);
        youScore.setSize(44);
        youScore.setLocalTranslation(10, settings.getHeight()-60,0);
        guiNode.attachChild(youScore);
        
        // result
        iWin = false;
        iLose = false;
        draw = false;

        resultTx = new BitmapText(guiFont);
        String re = "";
        resultTx.setText(re);
        resultTx.setColor(ColorRGBA.White);
        resultTx.setSize(56);
        resultTx.setLocalTranslation(settings.getWidth() / 2 - 100,settings.getHeight() / 2 + 350,0);
        guiNode.attachChild(resultTx);
        
        resultTx2 = new BitmapText(guiFont);
        String re2 = "";
        resultTx2.setText(re2);
        resultTx2.setSize(32);
        resultTx2.setColor(ColorRGBA.White);
        resultTx2.setLocalTranslation(settings.getWidth() / 2 - 225, settings.getHeight() / 2 + 290,0);
        guiNode.attachChild(resultTx2);
        
        // cross icon
        initCrossHairs();
        // set audio
        initAudio();
    }
    
    @Override
    public void simpleUpdate(float tpf) {
        // player move
        inputManager.setCursorVisible(false);
        Vector3f camDir = cam.getDirection().mult(0.2f);
        Vector3f camLeft = cam.getLeft().mult(0.2f);
        camDir.y = 0;
        camLeft.y = 0;
        viewDirection.set(camDir);
        walkDirection.set(0, 0, 0);
        if (leftStrafe && canPlay) {
            walkDirection.addLocal(camLeft);
        } else
        if (rightStrafe && canPlay) {
            walkDirection.addLocal(camLeft.negate());
        }
        if (leftRotate && canPlay) {
            viewDirection.addLocal(camLeft.mult(tpf));
        } else
        if (rightRotate && canPlay) {
            viewDirection.addLocal(camLeft.mult(tpf).negate());
        }
        if (forward && canPlay) {
            walkDirection.addLocal(camDir);
        } else
        if (backward && canPlay) {
            walkDirection.addLocal(camDir.negate());
        }
        myPhysicsCharacter.setWalkDirection(walkDirection);
        myPhysicsCharacter.setViewDirection(viewDirection);
        listener.setLocation(cam.getLocation());
        listener.setRotation(cam.getRotation());
        youCharacterNode.setLocalTranslation(youPos);
        Vector3f myPosb = cam.getLocation();
        // send data(about position)
        if (myPosb != myPos) {
            String msg_pos = "MOVE" + " " + myNumber + " " + 
                    myPosb.x + " " + myPosb.y + " " + myPosb.z;
            out.println(msg_pos);
            out.flush();
        }
        
        // set display
        String txt = "You: " + myShot + " / 5";
        myScore.setText(txt);
        String rt = "Rival: " + youShot + " / 5";
        youScore.setText(rt);

        // visible result
        if (iWin || iLose || draw) {
            canPlay = false;
            if (draw) {
                String drtx = "DRAW";
                resultTx.setText(drtx);
            } else if (iWin) {
                String wintx = "You Win!";
                resultTx.setText(wintx);
            } else {
                String lotx = "You Lose...";
                resultTx.setText(lotx);
            }
            resultTx2.setText("Please close this window(ESCキー).");
        }
    }

    // get key input
    @Override
    public void onAction(String binding, boolean value, float tpf) {
        if (binding.equals("Strafe Left") && canPlay) {
            if (value) {
                leftStrafe = true;
            } else {
                leftStrafe = false;
            }
        } else if (binding.equals("Strafe Right") && canPlay) {
            if (value) {
                rightStrafe = true;
            } else {
                rightStrafe = false;
            }
        } else if (binding.equals("Rotate Left") && canPlay) {
            if (value) {
                leftRotate = true;
            } else {
                leftRotate = false;
            }
        } else if (binding.equals("Rotate Right") && canPlay) {
            if (value) {
                rightRotate = true;
            } else {
                rightRotate = false;
            }
        } else if (binding.equals("Walk Forward") && canPlay) {
            if (value) {
                forward = true;
            } else {
                forward = false;
            }
        } else if (binding.equals("Walk Backward") && canPlay) {
            if (value) {
                backward = true;
            } else {
                backward = false;
            }
        } else if (binding.equals("Jump") && canPlay) {
            if (value) {
                myPhysicsCharacter.jump();
            }
            
        } else if (binding.equals("Shoot") && canPlay) {
            if(value) {
                makeCannonBall();
            }
                
        }
    }

    // set sound
    private void initAudio() {
        // shot sound
        audio_cannon = new AudioNode(assetManager, "resources/Sounds/shoot.wav", DataType.Buffer);
        audio_cannon.setPositional(false);
        audio_cannon.setLooping(false);
        audio_cannon.setVolume(3);
        rootNode.attachChild(audio_cannon);

        // bgm
        AudioNode audio_bgm = new AudioNode(assetManager, "resources/Sounds/bgm.wav", DataType.Buffer);
        audio_bgm.setLooping(true);
        audio_bgm.setPositional(false);
        audio_bgm.setVolume(2);
        rootNode.attachChild(audio_bgm);
        audio_bgm.play();
    }
  
    // collision
    private PhysicsSpace getPhysicsSpace() {
        return bulletAppState.getPhysicsSpace();
    }
  
    // create and add physics:cannon
    public void makeCannonBall() {
        Vector3f camDir = cam.getDirection();
        Vector3f camLoc = cam.getLocation();
        Vector3f dir = new Vector3f(0, 0, 0);
        Vector3f loc = new Vector3f(0, 0, 0);
        CollisionResults results = new CollisionResults();
        Ray ray = new Ray(cam.getLocation(), cam.getDirection());
        dir.set(camDir);
        loc.set(camLoc);

        PointLight gunLight = new PointLight();
        gunLight.setRadius(100f);
        gunLight.setColor(ColorRGBA.White.mult(7.5f)); 
        gunLight.setPosition(loc);
        rootNode.addLight(gunLight);
        shootables.collideWith(ray, results);

        if (results.size() > 0) {
            // if hit rival
            String closest = results.getClosestCollision().getGeometry().getParent().getName();
            System.out.println("You Shot:" + closest);
            if (closest.equals("Ogre_Skin-ogremesh")) {
                // send data(hit)
                String msg_shot = "SHOT" + " " + myNumber + " 1";
                out.println(msg_shot);
                out.flush();
            } else {
                String msg_shot = "SHOT" + " " + myNumber + " 0";
                out.println(msg_shot);
                out.flush();
            }
        } else {
            String msg_shot = "SHOT" + " " + myNumber + " 0";
            out.println(msg_shot);
            out.flush();
        }
        audio_cannon.playInstance(); // play SE
    }
   

    // aiming icon
    protected void initCrossHairs() {
        setDisplayStatView(false);
        BitmapText ch = new BitmapText(guiFont, false);
        ch.setText("+");        // fake crosshairs
        ch.setLocalTranslation( // center
        settings.getWidth() / 2 - 15, settings.getHeight() / 2 + 20, 0);
        guiNode.attachChild(ch);
    }
    
    // shutdown with esc key
    @Override
    public void destroy() {
        super.destroy();
        System.exit(0);
    }
}