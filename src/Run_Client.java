import java.awt.Color;

public class Run_Client {
  public static void main(String[] args){
    OmokClient client = new OmokClient("네트워크 오목 게임");
    client.setBackground(new Color(72, 84, 96));
    client.setVisible(true);
    client.connect();
  }
}