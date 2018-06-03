import java.io.*;

public class BTree{

  // 抽象クラス
  private abstract class Node{
    int serial;
  }

  // Btreeの内部節
  private class InternalNode extends Node{
    int nChilds; // この節が持っている子の数
    Node[] child; // 部分木
    Comparable[] low; // 各部分木の最小の要素

    // コンストラクタ
    private InternalNode(){
      serial = serialNumber++;
      nChilds = 0;
      child = new Node[MAX_CHILD];
      low = new Comparable[MAX_CHILD];
    }

    // keyを持つデータは何番目の部分木に入るかを調べる
    private int locateSubtree(Comparable key){
      for (int i=nChilds-1; i > 0; i--){
        if (key.compareTo(low[i]) >= 0){
          return i;
        }
      }
      return 0;
    }
  }

  //BTreeの葉
  private class Leaf extends Node{
    Comparable key; // 葉が持っているキーの値
    Object data; // 葉が格納するデータ

    // コンストラクタ
    private Leaf(Comparable aKey, Object aData){
      serial = serialNumber++;
      key = aKey;
      data = aData;
    }
  }

  private Node root; // 二分探索木の根
  private int serialNumber = 0; // nodeに付番するシリアル番号
  private Leaf currentLeaf;
  final private static int MAX_CHILD = 5; // 5階のBTree
  final private static int HALF_CHILD = ((MAX_CHILD+1)/2);

  // deleteAuxの戻り値
  final private static int OK = 1;
  final private static int OK_REMOVED = 2;
  final private static int OK_NEED_REORG = 3;
  final private static int NOT_FOUND = 4;

  // コンストラクタ 空のBTreeを生成する
  public BTree(){
    root = null;
  }

  // BTreeからkeyを探索
  public boolean search(Comparable key){
    currentLeaf = null;

    if (root == null){
      return false;
    } else {
      Node p = root;
      int i;
      while (p instanceof InternalNode){
        InternalNode node = (InternalNode)p;
        i = node.locateSubtree(key);
        p = node.child[i];
      }

      if (key.compareTo(((Leaf)p).key) == 0){
        currentLeaf = (Leaf)p;
        return true;
      } else {
        return false;
      }
    }
  }

  public Object getData(){
    if (currentLeaf == null){
      return null;
    } else {
      return currentLeaf.data;
    }
  }

  public boolean setData(Object data) {
    if (currentLeaf == null){
      return false;
    } else {
      currentLeaf.data = data;
      return true;
    }
  }

  private class InsertAuxResult{
    Node newNode;
    Comparable lowest;

    private InsertAuxResult(Node aNewNode, Comparable theLowest){
      newNode = aNewNode;
      lowest = theLowest;
    }
  }

  private InsertAuxResult insertAux(InternalNode pnode, int nth, Comparable key, Object data){
    InsertAuxResult result;
    int lowest;
    Node thisNode;

    if (pnode == null){
      thisNode = root;
    } else {
      thisNode = pnode.child[nth];
    }

    if (thisNode instanceof Leaf){
      Leaf leaf = (Leaf)thisNode;

      if (leaf.key.compareTo(key) == 0){
        return null;
      } else {
        Leaf newLeaf = new Leaf(key, data);

        if (key.compareTo(leaf.key) < 0){
          if (pnode == null){
            root = newLeaf;
          } else {
            pnode.child[nth] = newLeaf;
          }

          result = new InsertAuxResult(leaf, leaf.key);
        } else {
          result = new InsertAuxResult(newLeaf, key);
        }

        return result;
      }
    } else {
      InternalNode node = (InternalNode)thisNode;
      int pos;
      pos = node.locateSubtree(key);
      result = insertAux(node, pos, key, data);

      if (result == null || result.newNode == null){
        return result;
      }

      if (node.nChilds < MAX_CHILD){
        for (int i = node.nChilds-1; i > pos; i--){
          node.child[i+1] = node.child[i];
          node.low[i+1] = node.low[i];
        }
        node.child[pos+1] = result.newNode;
        node.low[pos+1] = result.lowest;
        node.nChilds++;
        return new InsertAuxResult(null, null);
      } else {
        InternalNode newNode = new InternalNode();

        if (pos < HALF_CHILD-1){
          for (int i = HALF_CHILD-1, j = 0; i < MAX_CHILD; i++, j++){
            newNode.child[j] = node.child[i];
            newNode.low[j] = node.low[i];
          }

          for (int i = HALF_CHILD-2; i > pos; i--){
            newNode.child[i+1] = node.child[i];
            newNode.low[i+1] = node.low[i];
          }

          node.child[pos+1] = result.newNode;
          node.low[pos+1] = result.lowest;
        } else {
          int j = MAX_CHILD - HALF_CHILD;

          for (int i = MAX_CHILD-1; i >= HALF_CHILD; i--){
            if (i == pos){
              node.child[j] = result.newNode;
              node.low[j--] = result.lowest;
            }
            newNode.child[j] = node.child[i];
            newNode.low[j--] = node.low[i];
          }

          if (pos < HALF_CHILD) {
            node.child[0] = result.newNode;
            node.low[0] = result.lowest;
          }
        }

        node.nChilds = HALF_CHILD;
        newNode.nChilds = (MAX_CHILD-1) - HALF_CHILD;

        return new InsertAuxResult(newNode, newNode.low[0]);
      }
    }
  }

  public boolean insert(Comparable key, Object data){
    currentLeaf = null;

    if (root == null){
      root = new Leaf(key, data);
      return true;
    } else {
      InsertAuxResult result = insertAux(null, -1, key, data);

      if (result == null){
        return false;
      }

      if (result.newNode != null){
        InternalNode newNode = new InternalNode();
        newNode.nChilds = 2;
        newNode.child[0] = root;
        newNode.child[1] = result.newNode;
        newNode.low[1] = result.lowest;
        root = newNode;
      }

      return true;
    }
  }

  // private boolean mergeNodes(InternalNode p, int x){
  //   InternalNode a;
  //   InternalNode b;
  //   int an;
  //   int bn;
  //
  //   a = (InternalNode)p.child[x];
  //   b = (InternalNode)p.child[i+1];
  //   b.low[0] = p.low[x+1];
  //   an = a.nChilds;
  //   bn = b.nChilds;
  //
  //   if (an + bn <= MAX_CHILD){
  //
  //   }
  // }

  public static void main(String[] args) throws IOException{
    BTree tree = new BTree();

    int[] data = {1, 100, 27, 45, 3, 135, 13};
    for (int i = 0; i < data.length; i++) {
      tree.insert(new Integer(data[i]), "["+data[i]+"]");
    }

    System.out.print(">");
    BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
    String str = null;
    while((str = input.readLine()) != null){
      if (str.length() == 0){
        System.out.print(">");
        continue;
      }

      char command = str.charAt(0);
      int i = 1;
      while(i < str.length() && str.charAt(i) == ' '){
        i++;
      }
      str = str.substring(i);

      if (command == 'q'){
        break;
      } else if (command == 'p'){
        System.out.println(tree);
      } else if (command == '=') {
        if (tree.setData(str)){
          System.out.println("値" + str + "の設定に成功しました。");
        } else {
          System.out.println("値" + str + "の設定に失敗しました。");
        }
      } else if (command == '+' || command == '/'){
        int num = 0;
        try{
          num = Integer.parseInt(str);
        } catch (NumberFormatException e){
          System.out.println("整数以外のものが指定されました" + str);
          continue;
        }

        if (command == '+'){
          if (tree.insert(new Integer(num), "[" + num + "]")){
            System.out.println(num + "の挿入に成功しました。");
          } else {
            System.out.println(num + "の挿入に失敗しました。");
          }
        } else if (command == '/'){
          if (tree.search(new Integer(num))){
            System.out.println(num + "が見つかりました。値=" + tree.getData());
          } else {
            System.out.println(num + "が見つかりませんでした。");
          }
        }
      } else {
        System.out.println("コマンド" + command + "はサポートされていません。");
      }

      System.out.print(">");
    }
  }
}
