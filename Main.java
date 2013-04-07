import java.util.*;
import java.io.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.Toolkit;
import javax.swing.event.*;
import javax.swing.tree.*;
import java.awt.Font;
import java.awt.Cursor;
import javax.swing.text.*;

import com.amazonaws.auth.*;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.transfer.*;
import com.amazonaws.services.s3.transfer.model.*;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

class BucketNode extends DefaultMutableTreeNode {
    Bucket data = null;
    BucketNode(Bucket bucket) {
        data = bucket;
        this.add(new DefaultMutableTreeNode(""));
    }
    public String toString() {
        return data.getName();
    }
}

class ObjectNode extends DefaultMutableTreeNode {
    S3ObjectSummary data = null;
    int nodeType = 0;
    ObjectNode(S3ObjectSummary object, int type) {
        data = object;
        nodeType = type;     
        if (nodeType == 1) {
            this.add(new DefaultMutableTreeNode(""));
        }
    }
    public String toString() {
        String s = data.getKey();
        int tmp = s.length() - nodeType;
        int t = s.lastIndexOf('/', tmp - 1);
        return s.substring(t + 1, tmp);
    }
}

class DataTree extends JTree implements TreeWillExpandListener, MouseListener {
    private AWSCredentials myCredentials;
    private AmazonS3 s3client;
    private DefaultTreeModel e3Model;
    private DefaultMutableTreeNode treeRoot;
    private TreePath clipboard = null;
    DataTree(String accessKey, String secretKey) {
        myCredentials = new BasicAWSCredentials(accessKey, secretKey);
        s3client = new AmazonS3Client(myCredentials);
        this.setRootVisible(true);
        this.addTreeWillExpandListener(this);
        this.addMouseListener(this);
        treeRoot = new DefaultMutableTreeNode("Buckets");
        e3Model = new DefaultTreeModel(treeRoot);
        this.setModel(e3Model);
        try {
            List<Bucket> bucketList = s3client.listBuckets();
            for (Bucket i : bucketList) {
                treeRoot.add(new BucketNode(i));
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Login Failed!");
        }
    }
    public void mouseExited(MouseEvent e) {
    }
    public void mouseEntered(MouseEvent e) {
    }
    public void mouseReleased(MouseEvent e) {
        if (e.isPopupTrigger()) {
            JTree tree = (JTree)e.getComponent();
            final TreePath path = tree.getPathForLocation(e.getX(), e.getY());
            tree.setSelectionPath(path);
            final Object node = path.getLastPathComponent();
            JPopupMenu menu = new JPopupMenu();
            JMenuItem iUpload = new JMenuItem("Upload");
            iUpload.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    s3client = new AmazonS3Client(myCredentials);
                    TransferManager trans = new TransferManager(s3client);
                    JFileChooser fc = new JFileChooser();
                    int res = fc.showOpenDialog(null);
                    if (res == JFileChooser.APPROVE_OPTION) {
                        File file = fc.getSelectedFile();
                        String key = "";
                        if (node instanceof ObjectNode) {
                            key += ((ObjectNode)node).data.getKey();
                        }
                        key += file.getName();
                        createBar(trans.upload(path.getPathComponent(1).toString(), key, file));
                        e3Model.nodeStructureChanged(treeRoot);
                        clipboard = null;
                    }
                }
            });
            JMenuItem iCut = new JMenuItem("Cut");
            iCut.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    clipboard = path;
                }
            });
            JMenuItem iPaste = new JMenuItem("Paste");
            iPaste.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    s3client = new AmazonS3Client(myCredentials);
                    String key = "";
                    Object node = path.getLastPathComponent();
                    if (node instanceof ObjectNode) {
                        key += ((ObjectNode)path.getLastPathComponent()).data.getKey();
                    }
                    key += clipboard.getLastPathComponent().toString();
                    Main.jf.setCursor(new Cursor(Cursor.WAIT_CURSOR));
                    s3client.copyObject(clipboard.getPathComponent(1).toString(), ((ObjectNode)clipboard.getLastPathComponent()).data.getKey(), path.getPathComponent(1).toString(), key);
                    s3client = new AmazonS3Client(myCredentials);
                    s3client.deleteObject(clipboard.getPathComponent(1).toString(), ((ObjectNode)clipboard.getLastPathComponent()).data.getKey());
                    clipboard = null;
                    Main.jf.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                    e3Model.nodeStructureChanged(treeRoot);
                }
            });
            JMenuItem iDownload = new JMenuItem("Download");
            iDownload.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    s3client = new AmazonS3Client(myCredentials);
                    TransferManager trans = new TransferManager(s3client);
                    JFileChooser fc = new JFileChooser();
                    int res = fc.showSaveDialog(null);
                    if (res == JFileChooser.APPROVE_OPTION) {
                        File file = fc.getSelectedFile();
                        createBar(trans.download(path.getPathComponent(1).toString(), ((ObjectNode)node).data.getKey(), file));
                    }
                }
            });
            JMenuItem iDelete = new JMenuItem("Delete");
            iDelete.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    s3client = new AmazonS3Client(myCredentials);
                    Main.jf.setCursor(new Cursor(Cursor.WAIT_CURSOR));
                    s3client.deleteObject(path.getPathComponent(1).toString(), ((ObjectNode)node).data.getKey());
                    JOptionPane.showMessageDialog(null, "Finish");
                    Main.jf.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                    e3Model.nodeStructureChanged(treeRoot);
                    clipboard = null;
                }
            });
            if (node instanceof BucketNode) {
                menu.add(iUpload);
                if (clipboard != null) {
                    menu.add(iPaste);
                }
                menu.show(tree, e.getX(), e.getY());
            } else if (node instanceof ObjectNode) {
                if (((ObjectNode)node).nodeType == 0) {
                    menu.add(iCut);
                    menu.add(iDownload);
                    menu.add(iDelete);
                    menu.show(tree, e.getX(), e.getY());
                } else {
                    menu.add(iUpload);
                    menu.add(iDelete);
                    if (clipboard != null) {
                        menu.add(iPaste);
                    }
                    menu.show(tree, e.getX(), e.getY());
                }
            }
        }
    }
    void createBar(Transfer trans) {
        JDialog jd = new JDialog();
        JProgressBar jp = new JProgressBar();
        TransBar tb = new TransBar(jp, trans, jd, Main.jf);
        Thread thread = new Thread(tb);
        thread.start();
        jd.setSize(240, 80);
        jd.setLocation(300, 200);
        jp.setSize(200, 15);
        jp.setLocation(10, 5);
        jd.setLayout(null);
        jp.setVisible(true);
        jd.add(jp);
        jp.setStringPainted(true);
        jp.setMinimum(0);
        jp.setMaximum((int)trans.getProgress().getTotalBytesToTransfer());
        jd.setVisible(true);
        Main.jf.setEnabled(false);
    }
    public void mousePressed(MouseEvent e) {
    }
    public void mouseClicked(MouseEvent e) {
    }
    public void treeWillCollapse(TreeExpansionEvent e) {
    }
    public void treeWillExpand(TreeExpansionEvent e) {
        Main.jf.setCursor(new Cursor(Cursor.WAIT_CURSOR));
        TreePath path = e.getPath();
        Object node = path.getLastPathComponent();
        if (node instanceof BucketNode) {
            BucketNode bucketNode = (BucketNode)(node);
            bucketNode.removeAllChildren();
            s3client = new AmazonS3Client(myCredentials);
            List<S3ObjectSummary> obj = s3client.listObjects(bucketNode.toString()).getObjectSummaries();
            for (S3ObjectSummary i : obj) {
                String key = i.getKey();
                int tmp = key.indexOf('/');
                if (tmp == key.length() - 1) {
                    bucketNode.add(new ObjectNode(i, 1));
                } else {
                    if (tmp == -1) {
                        bucketNode.add(new ObjectNode(i, 0));
                    }
                }
            }
        } else if (node instanceof ObjectNode) {
            ObjectNode objectNode = (ObjectNode)(node);
            objectNode.removeAllChildren();
            s3client = new AmazonS3Client(myCredentials);
            List<S3ObjectSummary> obj = s3client.listObjects(path.getPathComponent(1).toString()).getObjectSummaries();
            String nowPath = objectNode.data.getKey();
            for (S3ObjectSummary i : obj) {
                String key = i.getKey();
                if (key.startsWith(nowPath) && !key.equals(nowPath)) {
                    objectNode.add(new ObjectNode(i, key.charAt(key.length() - 1) == '/' ? 1 : 0));
                }
            }
        }
        Main.jf.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    }
}

class TransBar implements Runnable{
    private JProgressBar jp;
    private Transfer trans;
    private JDialog jd;
    private JFrame jf;
    TransBar(JProgressBar jp, Transfer trans, JDialog jd, JFrame jf) {
        this.jp = jp;
        this.trans = trans;
        this.jd = jd;
        this.jf = jf;
    }
    public void run() {
        Runnable runner = new Runnable() {
            public void run() {
                TransferProgress tp = trans.getProgress();
                jp.setValue((int)trans.getProgress().getBytesTransfered());
            }
        };
        while(!trans.isDone()) {
            try {
                SwingUtilities.invokeAndWait(runner);
                Thread.sleep(100);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        jp.setValue(jp.getMaximum());
        JOptionPane.showMessageDialog(null, "Finish");
        jf.setEnabled(true);
        jd.dispose();
    }
}

class LoginButton extends JButton implements ActionListener {
    LoginButton() {
        this.setLocation(20, 160);
        this.setSize(90, 30);
        this.setText("Login");
        this.addActionListener(this);
    }
    public void actionPerformed(ActionEvent e) {
        Main.jf.setCursor(new Cursor(Cursor.WAIT_CURSOR));
        Main.jf.dataTree = new DataTree(Main.jf.af.getText(), Main.jf.sf.getText());
        Main.jf.js.setViewportView(Main.jf.dataTree);
        Main.jf.outButton.setEnabled(true);
        Main.jf.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        this.setEnabled(false);
    }
}

class LogoutButton extends JButton implements ActionListener {
    LogoutButton() {
        this.setLocation(135, 160);
        this.setSize(90, 30);
        this.setText("Logout");
        this.addActionListener(this);
        this.setEnabled(false);
    }
    public void actionPerformed(ActionEvent e) {
        Main.jf.dataTree = null;
        Main.jf.js.setViewportView(Main.jf.dataTree);
        Main.jf.inButton.setEnabled(true);
        this.setEnabled(false);
    }
}

class AccessKeyIdField extends JTextField {
    AccessKeyIdField() {
        this.setSize(124, 20);
        this.setLocation(20, 50);
        this.setFont(new Font("Monospaced", Font.PLAIN, 12));
        this.setDocument(new PlainDocument() {
            public void insertString(int n, String s, AttributeSet a) throws BadLocationException {
                if (this.getLength() + s.length() > 20) {
                } else {
                    super.insertString(n, s, a);
                    Toolkit.getDefaultToolkit().beep();
                }
            }
        });
        this.setText("AKIAJJFM2OICN5ORSXFQ");
    }
}

class SecretKeyField extends JTextField {
    SecretKeyField() {
        this.setSize(205, 20);
        this.setLocation(20, 110);
        this.setFont(new Font("Monospaced", Font.PLAIN, 10));
        this.setDocument(new PlainDocument() {
            public void insertString(int n, String s, AttributeSet a) throws BadLocationException {
                if (this.getLength() + s.length() > 40) {
                } else {
                    super.insertString(n, s, a);
                    Toolkit.getDefaultToolkit().beep();
                }
            }
        });
        this.setText("dwG4N7uwByASYD/d93RB9KT8EhbyMF/gATOnGfCE");
    }
}

class MainFrame extends JFrame{
    DataTree dataTree = null;
    AccessKeyIdField af = new AccessKeyIdField();
    SecretKeyField sf = new SecretKeyField();
    JScrollPane js = new JScrollPane();
    LoginButton inButton = new LoginButton();
    LogoutButton outButton = new LogoutButton();
    MainFrame() {
        this.setTitle("Amazon S3 Client");
        this.setSize(480, 300);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLayout(null);
        Toolkit tk = Toolkit.getDefaultToolkit();
        int width = tk.getScreenSize().width;
        int height = tk.getScreenSize().height;
        this.setLocation((tk.getScreenSize().width - 480) / 2, (tk.getScreenSize().height - 300) / 2);
        JLabel jl0 = new JLabel("AWSAccessKeyID");
        JLabel jl1 = new JLabel("AWSSecretKey");
        jl0.setLocation(20, 25);
        jl0.setSize(120, 20);
        jl1.setLocation(20, 85);
        jl1.setSize(120, 20);
        this.add(jl0);
        this.add(jl1);
        this.add(inButton);
        this.add(outButton);
        this.add(af);
        this.add(sf);
        js.setSize(200, 200);
        js.setLocation(240, 30);
        this.add(js);
        this.setVisible(true);
    }
}

public class Main {
    static MainFrame jf;
    public static void main(String[] args) {
        MainFrame mainFrame = new MainFrame();
        jf = mainFrame;
    }
}

