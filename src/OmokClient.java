import java.awt.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.awt.event.*;

public class OmokClient extends Frame implements Runnable, ActionListener{
  private TextArea msgView = new TextArea("", 1,1,1);   // 메시지를 보여주는 영역
  private TextField sendBox = new TextField("");         // 보낼 메시지를 적는 상자
  private TextField nameBox = new TextField();          // 사용자 이름 상자
  private TextField roomBox = new TextField("0");        // 방 번호 상자

  // 방에 접속한 인원의 수를 보여주는 레이블
  private Label pInfo = new Label("대기실:  명");
  private java.awt.List pList = new java.awt.List();             // 사용자 명단을 보여주는 리스트
  private Button startButton = new Button("대국 시작");    // 대국 시작 버튼
  private Button stopButton = new Button("기권");         // 기권 버튼
  private Button enterButton = new Button("입장하기");    // 입장하기 버튼
  private Button exitButton = new Button("대기실로");     // 대기실로 버튼

  // 각종 정보를 보여주는 레이블
  private Label infoView = new Label("< Term Project : Omok >", 1);
  private OmokBoard board = new OmokBoard(15,30);          // 오목판 객체
  private BufferedReader reader;                                // 입력 스트림
  private PrintWriter writer;                                   // 출력 스트림
  private Socket socket;                                        // 소켓
  private int roomNumber = -1;                                  // 방 번호
  private String userName = null;                               // 사용자 이름
  public OmokClient(String title) {                             // 생성자
    super(title);
    setLayout(null);                                       // 레이아웃을 사용하지 않는다.

    // 각종 컴포넌트를 생성하고 배치한다.
    msgView.setEditable(false);
    infoView.setBounds(10,30,480,30);
    infoView.setBackground(new Color(200,200,255));
    board.setLocation(10,70);
    add(infoView);
    add(board);

    Panel p=new Panel();
    p.setBackground(new Color(200, 255, 255));
    p.setLayout(new GridLayout(3, 3));
    p.add(new Label("이     름:", 2)); p.add(nameBox);
    p.add(new Label("방 번호:", 2));   p.add(roomBox);
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

    // 이벤트 리스너를 등록한다.
    sendBox.addActionListener(this);
    enterButton.addActionListener(this);
    exitButton.addActionListener(this);
    startButton.addActionListener(this);
    stopButton.addActionListener(this);

    // 윈도우 닫기 처리
    addWindowListener(new WindowAdapter(){
       public void windowClosing(WindowEvent we){
         System.exit(0);
       }
    });
  }

 
  // 컴포넌트들의 액션 이벤트 처리
  public void actionPerformed(ActionEvent ae){
    // 메시지 입력 상자이면
    if(ae.getSource() == sendBox){             
      String msg = sendBox.getText();
      if(msg.length() == 0) return;
      if(msg.length() >= 30) msg = msg.substring(0,30);
      try{  
        writer.println("[MSG]"+msg);
        sendBox.setText("");
      } catch(Exception ie){}
    }
  
    // 입장하기 버튼이면
    else if(ae.getSource()==enterButton){         
      try{
        if(Integer.parseInt(roomBox.getText())<1){
          infoView.setText("방번호가 잘못되었습니다. 1이상");
          return;
        }
          writer.println("[ROOM]"+Integer.parseInt(roomBox.getText()));
          msgView.setText("");
        }catch(Exception ie){
          infoView.setText("입력하신 사항에 오류가 았습니다.");
        }
    }

    // 대기실로 버튼이면
    else if(ae.getSource() == exitButton){           
      try{
        goToWaitRoom();
        startButton.setEnabled(false);
        stopButton.setEnabled(false);
      } catch(Exception e){}
    }

    // 대국 시작 버튼이면
    else if(ae.getSource() == startButton){          
      try{
        writer.println("[START]");
        infoView.setText("상대의 결정을 기다립니다.");
        startButton.setEnabled(false);
      }catch(Exception e){}
    }

    // 기권 버튼이면
    else if(ae.getSource()==stopButton){          
      try{
        writer.println("[DROPGAME]");
        endGame("기권하였습니다.");
      }catch(Exception e){}
    }
  }

  // 대기실로 버튼을 누르면 호출된다.
  void goToWaitRoom(){                   
    if(userName == null){
      String name = nameBox.getText().trim();
      if(name.length() <= 2 || name.length() > 10){
        infoView.setText("이름이 잘못되었습니다. 3~10자");
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
    infoView.setText("대기실에 입장하셨습니다.");
    roomBox.setText("0");
    enterButton.setEnabled(true);
    exitButton.setEnabled(false);
  }

  public void run(){
    String msg;                                            // 서버로부터의 메시지
    try{
      while((msg=reader.readLine())!=null){
        // 상대편이 놓은 돌의 좌표
        if(msg.startsWith("[STONE]")){     
          String temp=msg.substring(7);
          int x=Integer.parseInt(temp.substring(0,temp.indexOf(" ")));
          int y=Integer.parseInt(temp.substring(temp.indexOf(" ")+1));
          board.putOpponent(x, y);                        // 상대편의 돌을 그린다.
          board.setEnable(true);                  // 사용자가 돌을 놓을 수 있도록 한다.
        }
        // 방에 입장
        else if(msg.startsWith("[ROOM]")){    
          if(!msg.equals("[ROOM]0")){           // 대기실이 아닌 방이면
            enterButton.setEnabled(false);
            exitButton.setEnabled(true);
            infoView.setText(msg.substring(6)+"번 방에 입장하셨습니다.");
          }
          else infoView.setText("대기실에 입장하셨습니다.");
          if(board.isRunning()){                         // 게임이 진행중인 상태이면
            board.stopGame();                            // 게임을 중지시킨다.
          }
        }
        // 방이 찬 상태이면
        else if(msg.startsWith("[FULL]")){       
          infoView.setText("방이 차서 입장할 수 없습니다.");
        }
        // 방에 있는 사용자 명단
        else if(msg.startsWith("[PLAYERS]")){      
          nameList(msg.substring(9));
        }
        // 손님 입장
        else if(msg.startsWith("[ENTER]")){        
          pList.add(msg.substring(7));
          playersInfo();
          msgView.append("[" +  msg.substring(7) + "]님이 입장하였습니다.\n");
        }
        // 손님 퇴장
        else if(msg.startsWith("[EXIT]")){            
          pList.remove(msg.substring(6));    // 리스트에서 제거
          playersInfo();                                // 인원수를 다시 계산하여 보여준다.
          msgView.append("[" + msg.substring(6) + "]님이 다른 방으로 입장하였습니다.\n");
          if(roomNumber!=0)
            endGame("상대가 나갔습니다.");
        }
        // 손님 접속 종료
        else if(msg.startsWith("[DISCONNECT]")){     
          pList.remove(msg.substring(12));
          playersInfo();
          msgView.append("[" + msg.substring(12) + "]님이 접속을 끊었습니다.\n");
          if(roomNumber!=0)
            endGame("상대가 나갔습니다.");
        }
        // 돌의 색을 부여받는다.
        else if(msg.startsWith("[COLOR]")){          
          String color=msg.substring(7);
          board.startGame(color);                         // 게임을 시작한다.
          if(color.equals("BLACK"))
            infoView.setText("흑돌을 잡았습니다.");
          else
            infoView.setText("백돌을 잡았습니다.");
          stopButton.setEnabled(true);                 // 기권 버튼 활성화
        }

        // 상대가 기권하면
        else if(msg.startsWith("[DROPGAME]"))      
          endGame("상대가 기권하였습니다.");

        // 이겼으면
        else if(msg.startsWith("[WIN]"))              
          endGame("이겼습니다.");

        // 졌으면
        else if(msg.startsWith("[LOSE]"))           
          endGame("졌습니다.");

        // 약속된 메시지가 아니면 메시지 영역에 보여준다.
        else msgView.append(msg + "\n");
      }
    }catch(IOException ie){
      msgView.append(ie+"\n");
    }
    msgView.append("접속이 끊겼습니다.");
  }

  // 게임의 종료시키는 메소드
  private void endGame(String msg){                
    infoView.setText(msg);
    startButton.setEnabled(false);
    stopButton.setEnabled(false);

    // 2초간 대기
    try{ Thread.sleep(2000); }catch(Exception e){}    
    if(board.isRunning())board.stopGame();
    if(pList.getItemCount()==2)startButton.setEnabled(true);
  }

  // 방에 있는 접속자의 수를 보여준다.
  private void playersInfo(){                 
    int count=pList.getItemCount();
    if(roomNumber==0)
      pInfo.setText("대기실: "+count+"명");
    else pInfo.setText(roomNumber+" 번 방: "+count+"명");

    // 대국 시작 버튼의 활성화 상태를 점검한다.
    if(count==2 && roomNumber!=0)
      startButton.setEnabled(true);
    else startButton.setEnabled(false);
  }

  // 사용자 리스트에서 사용자들을 추출하여 pList에 추가한다.
  private void nameList(String msg){
    pList.removeAll();
    StringTokenizer st=new StringTokenizer(msg, "\t");
    while(st.hasMoreElements())
      pList.add(st.nextToken());
    playersInfo();
  }

  // 연결
  void connect(){                   
    try{
      msgView.append("서버에 연결을 요청합니다.\n");
      socket = new Socket("localhost", 7777);
      msgView.append("=== 연결 성공 ===\n");
      msgView.append("이름을 입력하고 대기실로 입장하세요.\n");
      reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      writer = new PrintWriter(socket.getOutputStream(), true);

      new Thread(this).start();
      board.setWriter(writer);
    }catch(Exception e){
      msgView.append(e+"\n\n연결 실패..\n");  
    }
  }
}