import java.net.*;
import java.io.*;
import java.util.*;

public class OmokServer{
  private ServerSocket server;
  private BManager bMan=new BManager();       // 메시지 방송자
  private Random rnd= new Random();           // 흑과 백을 임의로 정하기 위한 변수
  public OmokServer(){}
  void startServer(){                         // 서버를 실행한다.
    try{
      server=new ServerSocket(7777);
      System.out.println("서버소켓이 생성되었습니다.");

      // 병렬 스레딩
      while(true){
        // 클라이언트와 연결된 스레드를 얻는다.
        Socket socket=server.accept();

        // 스레드를 만들고 실행시킨다.
        Omok_Thread ot=new Omok_Thread(socket);
        ot.start();

        // bMan에 스레드를 추가한다.
        bMan.add(ot);

        System.out.println("접속자 수: "+bMan.size());
      }
    }catch(Exception e){
      System.out.println(e);
    }
  }

 // 클라이언트와 통신하는 스레드 클래스
  class Omok_Thread extends Thread{
    private int roomNumber=-1;          // 방 번호
    private String userName=null;       // 사용자 이름
    private Socket socket;              // 소켓

    // 게임 준비 여부, true이면 게임을 시작할 준비가 되었음을 의미한다.
    private boolean ready=false;
    private BufferedReader reader;     // 입력 스트림
    private PrintWriter writer;        // 출력 스트림

    // 생성자
    Omok_Thread(Socket socket){       
      this.socket=socket;
    }

    // 소켓을 반환한다.
    Socket getSocket(){               
      return socket;
    }

    // 방 번호를 반환한다.
    int getRoomNumber(){              
      return roomNumber;
    }

    // 사용자 이름을 반환한다.
    String getUserName(){             
      return userName;
    }

     // 준비 상태를 반환한다.
    boolean isReady(){               
      return ready;
    }

    public void run(){
      try{
        reader=new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer=new PrintWriter(socket.getOutputStream(), true);

        String msg;                     // 클라이언트의 메시지

        while((msg=reader.readLine())!=null){
          // msg가 "[NAME]"으로 시작되는 메시지이면
          if(msg.startsWith("[NAME]")){
            userName=msg.substring(6);          // userName을 정한다.
          }

          // msg가 "[ROOM]"으로 시작되면 방 번호를 정한다.
          else if(msg.startsWith("[ROOM]")){
            int roomNum=Integer.parseInt(msg.substring(6));
            if( !bMan.isFull(roomNum)){             // 방이 찬 상태가 아니면
              // 현재 방의 다른 사용에게 사용자의 퇴장을 알린다.
              if(roomNumber!=-1)
                bMan.sendToOthers(this, "[EXIT]"+userName);

              // 사용자의 새 방 번호를 지정한다.
              roomNumber=roomNum;

              // 사용자에게 메시지를 그대로 전송하여 입장할 수 있음을 알린다.
              writer.println(msg);

              // 사용자에게 새 방에 있는 사용자 이름 리스트를 전송한다.
              writer.println(bMan.getNamesInRoom(roomNumber));

              // 새 방에 있는 다른 사용자에게 사용자의 입장을 알린다.
              bMan.sendToOthers(this, "[ENTER]"+userName);
            }
            else writer.println("[FULL]");        // 사용자에 방이 찼음을 알린다.
          }

 

          // "[STONE]" 메시지는 상대편에게 전송한다.
          else if(roomNumber>=1 && msg.startsWith("[STONE]"))
            bMan.sendToOthers(this, msg);

          // 대화 메시지를 방에 전송한다.
          else if(msg.startsWith("[MSG]"))
            bMan.sendToRoom(roomNumber, "["+userName+"]: "+msg.substring(5));

          // "[START]" 메시지이면
          else if(msg.startsWith("[START]")){
            ready=true;   // 게임을 시작할 준비가 되었다.

            // 다른 사용자도 게임을 시작한 준비가 되었으면
            if(bMan.isReady(roomNumber)){
              // 흑과 백을 정하고 사용자와 상대편에게 전송한다.
              int a=rnd.nextInt(2);

              if(a==0){
                writer.println("[COLOR]BLACK");
                bMan.sendToOthers(this, "[COLOR]WHITE");
              }
              else{
                writer.println("[COLOR]WHITE");
                bMan.sendToOthers(this, "[COLOR]BLACK");
              }
            }
          }

          // 사용자가 게임을 중지하는 메시지를 보내면
          else if(msg.startsWith("[STOPGAME]"))
            ready=false;

          // 사용자가 게임을 기권하는 메시지를 보내면
          else if(msg.startsWith("[DROPGAME]")){
            ready=false;
            // 상대편에게 사용자의 기권을 알린다.
            bMan.sendToOthers(this, "[DROPGAME]");
          }

          // 사용자가 이겼다는 메시지를 보내면
          else if(msg.startsWith("[WIN]")){
            ready=false;
            
            // 사용자에게 메시지를 보낸다.
            writer.println("[WIN]");
 
            // 상대편에는 졌음을 알린다.
            bMan.sendToOthers(this, "[LOSE]");
          }  
        }

      } catch(Exception e){} 
      finally{
        try{
          bMan.remove(this);
          if(reader!=null) reader.close();
          if(writer!=null) writer.close();
          if(socket!=null) socket.close();
          reader=null; writer=null; socket=null;
          System.out.println(userName+"님이 접속을 끊었습니다.");
          System.out.println("접속자 수: "+bMan.size());

          // 사용자가 접속을 끊었음을 같은 방에 알린다.
          bMan.sendToRoom(roomNumber,"[DISCONNECT]"+userName);
        }catch(Exception e){}
      }
    }
  }

  class BManager extends Vector{       // 메시지를 전달하는 클래스
    BManager(){}

    // 스레드를 추가한다
    void add(Omok_Thread ot){           
      super.add(ot);
    }

    // 스레드를 제거한다.
    void remove(Omok_Thread ot){        
       super.remove(ot);
    }

    // i번째 스레드를 반환한다.
    Omok_Thread getOT(int i){            
      return (Omok_Thread)elementAt(i);
    }

    // i번째 스레드의 소켓을 반환한다.
    Socket getSocket(int i){              
      return getOT(i).getSocket();
    }

    // i번째 스레드와 연결된 클라이언트에게 메시지를 전송한다.
    void sendTo(int i, String msg){
      try{
        PrintWriter pw= new PrintWriter(getSocket(i).getOutputStream(), true);
        pw.println(msg);
      }catch(Exception e){}  
    }

    // i번째 스레드의 방 번호를 반환한다.
    int getRoomNumber(int i){            
      return getOT(i).getRoomNumber();
    }

    // 방이 찼는지 알아본다.
    synchronized boolean isFull(int roomNum){    
      // 대기실은 차지 않는다.
      if(roomNum==0)return false;                 

      // 다른 방은 2명 이상 입장할 수 없다.
      int count=0;
      for(int i=0;i<size();i++)
        if(roomNum==getRoomNumber(i))count++;
      if(count>=2)return true;
      return false;
    }

    // roomNum 방에 msg를 전송한다.
    void sendToRoom(int roomNum, String msg){
      for(int i=0;i<size();i++)
        if(roomNum==getRoomNumber(i))
          sendTo(i, msg);
    }

    // ot와 같은 방에 있는 다른 사용자에게 msg를 전달한다.
    void sendToOthers(Omok_Thread ot, String msg){
      for(int i=0;i<size();i++)
        if(getRoomNumber(i)==ot.getRoomNumber() && getOT(i)!=ot)
          sendTo(i, msg);
    }

    // 게임을 시작할 준비가 되었는가를 반환한다.
    // 두 명의 사용자 모두 준비된 상태이면 true를 반환한다.
    synchronized boolean isReady(int roomNum){
      int count=0;
      for(int i=0;i<size();i++)
        if(roomNum==getRoomNumber(i) && getOT(i).isReady())
          count++;

      if(count==2) 
        return true;

      return false;
    }

 

    // roomNum방에 있는 사용자들의 이름을 반환한다.
    String getNamesInRoom(int roomNum){
      StringBuffer sb=new StringBuffer("[PLAYERS]");
      for(int i = 0; i < size(); i++)
        if(roomNum == getRoomNumber(i))
          sb.append(getOT(i).getUserName() + "\t");
      return sb.toString();
    }
  }
}