import java.awt.*;
import java.io.*;
import java.awt.event.*;

// 오목판을 구현하는 클래스
class OmokBoard extends Canvas{               
  public static final int BLACK = 1, WHITE = -1;   // 흑과 백을 나타내는 상수
  
  // SIZE : 격자의 가로 또는 세로 개수 ( 19 x 19 )
  // CELL : 격자의 크기(pixel)
  public static final int CELL = 30, SIZE = 20, STONE_SIZE = CELL - 2;

  private int[][] Map;                         // 오목판 배열
  private String info = "게임 중지";           // 게임의 진행 상황을 나타내는 문자열
  private int color = BLACK;                   // 사용자의 돌 색깔

  // true이면 사용자가 돌을 놓을 수 있는 상태를 의미하고,
  // false이면 사용자가 돌을 놓을 수 없는 상태를 의미한다.
  private boolean enable = false;

  private boolean running = false;            // 게임이 진행 중인가를 나타내는 변수
  private PrintWriter writer;                 // 상대편에게 메시지를 전달하기 위한 스트림
  private Graphics g = null;                  // 캔버스와 버퍼를 위한 Graphics 객체
  private Image buff;                         // 더블 버퍼링을 위한 버퍼

  // 오목판의 생성자
  OmokBoard(){           
    Map = new int[SIZE + 2][];                // 맵의 크기를 정한다.
    for(int i = 0;i < Map.length; i++)
      Map[i] = new int[SIZE + 2];

    setBackground(new Color(53, 59, 72));                // 오목판의 배경색을 정한다.
    setSize(SIZE*(CELL + 1) + SIZE, SIZE*(CELL + 1) + SIZE);      // 오목판의 크기를 계산한다. (Width, Height)

    // 오목판의 마우스 이벤트 처리
    addMouseListener(new MouseAdapter(){
      public void mousePressed(MouseEvent e){       // 마우스를 누르면
        if(!enable) return;                         // 사용자가 누를 수 없는 상태면 빠져 나온다.

        // 마우스의 좌표를 Map 좌표로 계산한다.
        int x = (int)Math.round(e.getX() / (double)CELL);
        int y = (int)Math.round(e.getY() / (double)CELL);

        // 돌이 놓일 수 있는 좌표가 아니면 빠져 나온다.
        if(x == 0 || y == 0 || x == SIZE + 1 || y == SIZE + 1) 
          return;

        // 해당 좌표에 다른 돌이 놓여져 있으면 빠져 나온다.
        if(Map[x][y] == BLACK || Map[x][y] == WHITE)
          return;

        // 상대편에게 놓은 돌의 좌표를 서버에 전송한다.
        writer.println("[STONE]" + x + " " + y);
        Map[x][y] = color;

        // 이겼는지 검사한다.
        if(checkWin(new Point(x, y), color)){
          info = "이겼습니다.";
          writer.println("[WIN]");
        }
        else info = "상대가 두기를 기다립니다.";
        repaint();                                   // 오목판을 그린다.

        // 사용자가 둘 수 없는 상태로 만든다.
        // 상대편이 두면 enable이 true가 되어 사용자가 둘 수 있게 된다.
        enable = false;
      }
    });
  }

  // 게임의 진행 상태를 반환한다.
  public boolean isRunning(){           
    return running;
  }

  // 게임을 시작
  public void startGame(String col){     
    running = true;

    // 흑이 선택되었을 때 (선공)
    if(col.equals("BLACK")){   
      enable = true; 
      color = BLACK;
      info = "게임 시작... 선공입니다.";
    }   

    // 백이 선택되었을 때 (후공)
    else{                                
      enable = false;
      color = WHITE;
      info = "게임 시작... 상대가 놓을 차례입니다.";
    }
  }

  // 게임을 멈춘다.
  public void stopGame(){               
    init();                             // 오목판을 초기화
    writer.println("[STOPGAME]");     // 상대편에게 메시지를 보낸다.
    enable = false;
    running = false;
    repaint();  // 수정
  }

  // 상대편의 돌을 놓는다.
  public void putOpponent(int x, int y){      
    // 상대방의 color 변수의 값은 반대 값일 것이다. (나 : 1 상대방 : -1)
    // 그렇다면 상대방에게 내 돌의 색깔과 같은 돌을 놓아주려면 상대방 color의 -1을 곱해줘야 한다. 
    Map[x][y] = -color;             
    info = "상대가 두었습니다. 두세요.";
    repaint();
  }

  public void setEnable(boolean enable){
    this.enable = enable;
  }

  public void setWriter(PrintWriter writer){
    this.writer = writer;
  }

  // repaint를 호출하면 자동으로 호출된다.
  public void update(Graphics graphic){        
    System.out.println("Repaint -ing");
    paint(graphic);                            // paint를 호출한다.
  }

  // 화면을 그린다.
  public void paint(Graphics graphic){                
    if(g == null){                   // 버퍼가 없으면 버퍼를 만든다.
      buff = createImage(getWidth(),getHeight());
      g = buff.getGraphics();
    }
    drawBoard(graphic);    // 오목판을 그린다.
  }

  // 오목판을 초기화시킨다.
  public void init(){                         
    for(int y = 0; y < Map.length; y++)
      for(int x = 0; x < Map[y].length; x++)
        Map[y][x] = 0;

    info = "게임 중지";
    repaint();
  }

  // 오목판에 선을 긋는다.
  private void drawLines(){                     
    g.setColor(Color.black);
    for(int i = 1; i <= SIZE; i++){
      // 가로 줄
      g.drawLine(CELL, i*CELL, CELL*SIZE, i*CELL);
      // 세로 줄
      g.drawLine(i*CELL, CELL, i*CELL , CELL*SIZE);
    }
  }

  // 흑 돌을 (x, y)에 그린다.
  private void drawBlack(int x, int y){ 
    int interval = CELL / 2;   
    g.setColor(Color.black);
    g.fillOval(x*CELL - interval, y*CELL - interval, STONE_SIZE, STONE_SIZE);
  }

  // 백 돌을 (x, y)에 그린다.
  private void drawWhite(int x, int y){
    int interval = CELL / 2;   
    g.setColor(Color.white);
    g.fillOval(x*CELL - interval, y*CELL - interval, STONE_SIZE, STONE_SIZE);
  }

  // Map 놓여진 돌들을 모두 그린다.
  private void drawStones(){                  
    for(int x = 1; x <= SIZE; x++)
     for(int y = 1; y <= SIZE; y++){
       if(Map[x][y] == BLACK)
         drawBlack(x, y);
       else if(Map[x][y] == WHITE)
         drawWhite(x, y);
     }
  }

  // 오목판을 그린다.
  synchronized private void drawBoard(Graphics g){      
    // 버퍼에 먼저 그리고 버퍼의 이미지를 오목판에 그린다.
    g.clearRect(0, 0, getWidth(), getHeight());
    drawLines();
    drawStones();
    g.setColor(Color.red);
    g.drawString(info, 20, 15);
    g.drawImage(buff, 0, 0, this);
  }

  // 승리했는지 확인한다.
  private boolean checkWin(Point p, int color){
    // 방향 벡터
    int dir[][] = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 }, { -1, 1 }, { 1, -1 }, { -1, -1 }, { 1, 1 } };
    int win_score = 1;

    for (int i = 0; i < 8; i += 2) {
      // win_score 가 5가 되면 승리 = 오목이 된 경우
      int cunX = p.x;
      int cunY = p.y;

      for(int j = 0; j < 5; j++) {
        cunX += dir[i][0];
        cunY += dir[i][1];

        // 종료 조건 1: Stone이 coord범위를 벗어난 경우
        if(cunX < 0 || cunY >= SIZE || cunY < 0 || cunX >= SIZE)
          break;
        
        // 종료 조건 2: Stone이 같은 색으로 이어지지 않은 경우
        else if(Map[cunX][cunY] != color)
          break;
        
        // 위 경우가 아니라면, win_score 1 증가
        else 
          win_score++;
      }
      
      cunX = p.x;
      cunY = p.y;
      
      for(int j = 0; j < 5; j++) {
          cunX += dir[i + 1][0];
          cunY += dir[i + 1][1];

        // 종료 조건 1: Stone이 coord범위를 벗어난 경우
        if(cunX < 0 || cunY >= SIZE || cunY < 0 || cunX >= SIZE)
          break;
        
        // 종료 조건 2: Stone이 같은 색으로 이어지지 않은 경우
        else if(Map[cunX][cunY] != color)
          break;
        
        // 위 경우가 아니라면, win_score 1 증가
        else 
          win_score++;
        }
      // 오목이 된 경우 true를 return 
      // 육목은 허용 X
      if(win_score == 5) {
        return true;
      }
    }
    System.out.println("Win_score : " + win_score);
    return false;
  }
}