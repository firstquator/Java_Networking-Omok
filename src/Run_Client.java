public class Run_Client {
  public static void main(String[] args){
    OmokClient client = new OmokClient("네트워크 오목 게임");
    client.setVisible(true);
    client.connect();
  }
}

