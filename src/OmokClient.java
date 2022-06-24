import java.awt.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.awt.event.*;

public class OmokClient extends Frame implements Runnable, ActionListener{
  private TextArea msgView = new TextArea("", 1,1,1);   // �޽����� �����ִ� ����
  private TextField sendBox = new TextField("");         // ���� �޽����� ���� ����
  private TextField nameBox = new TextField();          // ����� �̸� ����
  private TextField roomBox = new TextField("0");        // �� ��ȣ ����

  // �濡 ������ �ο��� ���� �����ִ� ���̺�
  private Label pInfo = new Label("����:  ��");
  private java.awt.List pList = new java.awt.List();             // ����� ����� �����ִ� ����Ʈ
  private Button startButton = new Button("�뱹 ����");    // �뱹 ���� ��ư
  private Button stopButton = new Button("���");         // ��� ��ư
  private Button enterButton = new Button("�����ϱ�");    // �����ϱ� ��ư
  private Button exitButton = new Button("���Ƿ�");     // ���Ƿ� ��ư

  // ���� ������ �����ִ� ���̺�
  private Label infoView = new Label("< Term Project : Omok >", 1);
  private OmokBoard board = new OmokBoard(15,30);          // ������ ��ü
  private BufferedReader reader;                                // �Է� ��Ʈ��
  private PrintWriter writer;                                   // ��� ��Ʈ��
  private Socket socket;                                        // ����
  private int roomNumber = -1;                                  // �� ��ȣ
  private String userName = null;                               // ����� �̸�
  public OmokClient(String title) {                             // ������
    super(title);
    setLayout(null);                                       // ���̾ƿ��� ������� �ʴ´�.

    // ���� ������Ʈ�� �����ϰ� ��ġ�Ѵ�.
    msgView.setEditable(false);
    infoView.setBounds(10,30,480,30);
    infoView.setBackground(new Color(200,200,255));
    board.setLocation(10,70);
    add(infoView);
    add(board);

    Panel p=new Panel();
    p.setBackground(new Color(200, 255, 255));
    p.setLayout(new GridLayout(3, 3));
    p.add(new Label("��     ��:", 2)); p.add(nameBox);
    p.add(new Label("�� ��ȣ:", 2));   p.add(roomBox);
    p.add(enterButton); p.add(exitButton);
    enterButton.setEnabled(false);
    p.setBounds(500,30, 250,70);

    Panel p2=new Panel();
    p2.setBackground(new Color(255,255,100));
    p2.setLayout(new BorderLayout());
    Panel p2_1=new Panel();
    p2_1.add(startButton); p2_1.add(stopButton);
    p2.add(pInfo,"North"); p2.add(pList,"Center"); p2.add(p2_1,"South");
    startButton.setEnabled(false); stopButton.setEnabled(false);
    p2.setBounds(500,110,250,180);

    Panel p3=new Panel();
    p3.setLayout(new BorderLayout());
    p3.add(msgView,"Center");
    p3.add(sendBox, "South");
    p3.setBounds(500, 300, 250,250);
    add(p); add(p2); add(p3);

    // �̺�Ʈ �����ʸ� ����Ѵ�.
    sendBox.addActionListener(this);
    enterButton.addActionListener(this);
    exitButton.addActionListener(this);
    startButton.addActionListener(this);
    stopButton.addActionListener(this);

    // ������ �ݱ� ó��
    addWindowListener(new WindowAdapter(){
       public void windowClosing(WindowEvent we){
         System.exit(0);
       }
    });
  }

 
  // ������Ʈ���� �׼� �̺�Ʈ ó��
  public void actionPerformed(ActionEvent ae){
    // �޽��� �Է� �����̸�
    if(ae.getSource() == sendBox){             
      String msg = sendBox.getText();
      if(msg.length() == 0) return;
      if(msg.length() >= 30) msg = msg.substring(0,30);
      try{  
        writer.println("[MSG]"+msg);
        sendBox.setText("");
      } catch(Exception ie){}
    }
  
    // �����ϱ� ��ư�̸�
    else if(ae.getSource()==enterButton){         
      try{
        if(Integer.parseInt(roomBox.getText())<1){
          infoView.setText("���ȣ�� �߸��Ǿ����ϴ�. 1�̻�");
          return;
        }
          writer.println("[ROOM]"+Integer.parseInt(roomBox.getText()));
          msgView.setText("");
        }catch(Exception ie){
          infoView.setText("�Է��Ͻ� ���׿� ������ �ҽ��ϴ�.");
        }
    }

    // ���Ƿ� ��ư�̸�
    else if(ae.getSource() == exitButton){           
      try{
        goToWaitRoom();
        startButton.setEnabled(false);
        stopButton.setEnabled(false);
      } catch(Exception e){}
    }

    // �뱹 ���� ��ư�̸�
    else if(ae.getSource() == startButton){          
      try{
        writer.println("[START]");
        infoView.setText("����� ������ ��ٸ��ϴ�.");
        startButton.setEnabled(false);
      }catch(Exception e){}
    }

    // ��� ��ư�̸�
    else if(ae.getSource()==stopButton){          
      try{
        writer.println("[DROPGAME]");
        endGame("����Ͽ����ϴ�.");
      }catch(Exception e){}
    }
  }

  // ���Ƿ� ��ư�� ������ ȣ��ȴ�.
  void goToWaitRoom(){                   
    if(userName == null){
      String name = nameBox.getText().trim();
      if(name.length() <= 2 || name.length() > 10){
        infoView.setText("�̸��� �߸��Ǿ����ϴ�. 3~10��");
        nameBox.requestFocus();
        return;
      }
      userName=name;
      writer.println("[NAME]" + userName);    
      nameBox.setText(userName);
      nameBox.setEditable(false);
    }  

    msgView.setText("");
    writer.println("[ROOM] 0");
    infoView.setText("���ǿ� �����ϼ̽��ϴ�.");
    roomBox.setText("0");
    enterButton.setEnabled(true);
    exitButton.setEnabled(false);
  }

  public void run(){
    String msg;                                            // �����κ����� �޽���
    try{
      while((msg=reader.readLine())!=null){
        // ������� ���� ���� ��ǥ
        if(msg.startsWith("[STONE]")){     
          String temp=msg.substring(7);
          int x=Integer.parseInt(temp.substring(0,temp.indexOf(" ")));
          int y=Integer.parseInt(temp.substring(temp.indexOf(" ")+1));
          board.putOpponent(x, y);                        // ������� ���� �׸���.
          board.setEnable(true);                  // ����ڰ� ���� ���� �� �ֵ��� �Ѵ�.
        }
        // �濡 ����
        else if(msg.startsWith("[ROOM]")){    
          if(!msg.equals("[ROOM]0")){           // ������ �ƴ� ���̸�
            enterButton.setEnabled(false);
            exitButton.setEnabled(true);
            infoView.setText(msg.substring(6)+"�� �濡 �����ϼ̽��ϴ�.");
          }
          else infoView.setText("���ǿ� �����ϼ̽��ϴ�.");
          if(board.isRunning()){                         // ������ �������� �����̸�
            board.stopGame();                            // ������ ������Ų��.
          }
        }
        // ���� �� �����̸�
        else if(msg.startsWith("[FULL]")){       
          infoView.setText("���� ���� ������ �� �����ϴ�.");
        }
        // �濡 �ִ� ����� ���
        else if(msg.startsWith("[PLAYERS]")){      
          nameList(msg.substring(9));
        }
        // �մ� ����
        else if(msg.startsWith("[ENTER]")){        
          pList.add(msg.substring(7));
          playersInfo();
          msgView.append("[" +  msg.substring(7) + "]���� �����Ͽ����ϴ�.\n");
        }
        // �մ� ����
        else if(msg.startsWith("[EXIT]")){            
          pList.remove(msg.substring(6));    // ����Ʈ���� ����
          playersInfo();                                // �ο����� �ٽ� ����Ͽ� �����ش�.
          msgView.append("[" + msg.substring(6) + "]���� �ٸ� ������ �����Ͽ����ϴ�.\n");
          if(roomNumber!=0)
            endGame("��밡 �������ϴ�.");
        }
        // �մ� ���� ����
        else if(msg.startsWith("[DISCONNECT]")){     
          pList.remove(msg.substring(12));
          playersInfo();
          msgView.append("[" + msg.substring(12) + "]���� ������ �������ϴ�.\n");
          if(roomNumber!=0)
            endGame("��밡 �������ϴ�.");
        }
        // ���� ���� �ο��޴´�.
        else if(msg.startsWith("[COLOR]")){          
          String color=msg.substring(7);
          board.startGame(color);                         // ������ �����Ѵ�.
          if(color.equals("BLACK"))
            infoView.setText("�浹�� ��ҽ��ϴ�.");
          else
            infoView.setText("�鵹�� ��ҽ��ϴ�.");
          stopButton.setEnabled(true);                 // ��� ��ư Ȱ��ȭ
        }

        // ��밡 ����ϸ�
        else if(msg.startsWith("[DROPGAME]"))      
          endGame("��밡 ����Ͽ����ϴ�.");

        // �̰�����
        else if(msg.startsWith("[WIN]"))              
          endGame("�̰���ϴ�.");

        // ������
        else if(msg.startsWith("[LOSE]"))           
          endGame("�����ϴ�.");

        // ��ӵ� �޽����� �ƴϸ� �޽��� ������ �����ش�.
        else msgView.append(msg + "\n");
      }
    }catch(IOException ie){
      msgView.append(ie+"\n");
    }
    msgView.append("������ ������ϴ�.");
  }

  // ������ �����Ű�� �޼ҵ�
  private void endGame(String msg){                
    infoView.setText(msg);
    startButton.setEnabled(false);
    stopButton.setEnabled(false);

    // 2�ʰ� ���
    try{ Thread.sleep(2000); }catch(Exception e){}    
    if(board.isRunning())board.stopGame();
    if(pList.getItemCount()==2)startButton.setEnabled(true);
  }

  // �濡 �ִ� �������� ���� �����ش�.
  private void playersInfo(){                 
    int count=pList.getItemCount();
    if(roomNumber==0)
      pInfo.setText("����: "+count+"��");
    else pInfo.setText(roomNumber+" �� ��: "+count+"��");

    // �뱹 ���� ��ư�� Ȱ��ȭ ���¸� �����Ѵ�.
    if(count==2 && roomNumber!=0)
      startButton.setEnabled(true);
    else startButton.setEnabled(false);
  }

  // ����� ����Ʈ���� ����ڵ��� �����Ͽ� pList�� �߰��Ѵ�.
  private void nameList(String msg){
    pList.removeAll();
    StringTokenizer st=new StringTokenizer(msg, "\t");
    while(st.hasMoreElements())
      pList.add(st.nextToken());
    playersInfo();
  }

  // ����
  void connect(){                   
    try{
      msgView.append("������ ������ ��û�մϴ�.\n");
      socket = new Socket("localhost", 7777);
      msgView.append("=== ���� ���� ===\n");
      msgView.append("�̸��� �Է��ϰ� ���Ƿ� �����ϼ���.\n");
      reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      writer = new PrintWriter(socket.getOutputStream(), true);

      new Thread(this).start();
      board.setWriter(writer);
    }catch(Exception e){
      msgView.append(e+"\n\n���� ����..\n");  
    }
  }
}