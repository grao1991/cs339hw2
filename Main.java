import java.util.*;
import java.io.*;
import javax.swing.*;
import java.awt.event.*;
import javax.swing.event.*;
import javax.swing.tree.*;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.*;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.transfer.*;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.PutObjectRequest;
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
    AWSCredentials myCredentials = new BasicAWSCredentials("AKIAJJFM2OICN5ORSXFQ", "dwG4N7uwByASYD/d93RB9KT8EhbyMF/gATOnGfCE");
    AmazonS3 s3client = new AmazonS3Client(myCredentials);
    DataTree() {
        this.setRootVisible(true);
        this.addTreeWillExpandListener(this);
        this.addMouseListener(this);
        DefaultMutableTreeNode treeRoot = new DefaultMutableTreeNode("Buckets");
        DefaultTreeModel e3Model = new DefaultTreeModel(treeRoot);
        this.setModel(e3Model);
        List<Bucket> bucketList = s3client.listBuckets();
        for (Bucket i : bucketList) {
            treeRoot.add(new BucketNode(i));
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
            Object node = path.getLastPathComponent();
            if (node instanceof BucketNode) {
            } else if (node instanceof ObjectNode) {
                final ObjectNode objectNode = (ObjectNode)node;
                if (objectNode.nodeType == 0) { // file
                    JPopupMenu menu = new JPopupMenu();
                    JMenuItem iDelete = new JMenuItem("Delete");
                    iDelete.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        String key = objectNode.data.getKey();
                        System.out.println(key);
                        BucketNode bucketNode = (BucketNode)path.getPathComponent(1);
                        s3client.deleteObject(bucketNode.data.getName(), key);
                        System.out.println(key);
                    }});
                    JMenuItem iCut = new JMenuItem("Cut");
                    JMenuItem iDownload = new JMenuItem("Download");
                    iDownload.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        TransferManager trans = new TransferManager(s3client);
                        JFileChooser fc = new JFileChooser();
                        int res = fc.showSaveDialog(null);
                        if (res == JFileChooser.APPROVE_OPTION) {
                            File file = fc.getSelectedFile();
                            String key = objectNode.data.getKey();
                            BucketNode bucketNode = (BucketNode)path.getPathComponent(1);
                            trans.download(bucketNode.data.getName(), key, file);
                        }
                    }});
                    menu.add(iDelete);
                    menu.add(iCut);
                    menu.add(iDownload);
                    menu.show(tree, e.getX(), e.getY());
                } else { // dir
                    JPopupMenu menu = new JPopupMenu();
                    JMenuItem iUpload = new JMenuItem("Upload");
                    iUpload.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        TransferManager trans = new TransferManager(s3client);
                        JFileChooser fc = new JFileChooser();
                        int res = fc.showOpenDialog(null);
                        if (res == JFileChooser.APPROVE_OPTION) {
                            File file = fc.getSelectedFile();
                            String name = file.getName();
                            String key = objectNode.data.getKey() + name;
                            BucketNode bucketNode = (BucketNode)path.getPathComponent(1);
                            trans.upload(bucketNode.data.getName(), key, file);
                        }
                    }});
                    menu.add(iUpload);
                    menu.show(tree, e.getX(), e.getY());
                }
            }
        }
    }
    public void mousePressed(MouseEvent e) {
    }
    public void mouseClicked(MouseEvent e) {
    }
    public void treeWillCollapse(TreeExpansionEvent e) {
    }
    public void treeWillExpand(TreeExpansionEvent e) {
        TreePath path = e.getPath();
        Object node = path.getLastPathComponent();
        if (node instanceof BucketNode) {
            BucketNode bucketNode = (BucketNode)(node);
            bucketNode.removeAllChildren();
            List<S3ObjectSummary> obj = s3client.listObjects(bucketNode.data.getName()).getObjectSummaries();
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
            BucketNode bucketNode = (BucketNode)(path.getPathComponent(1));
            List<S3ObjectSummary> obj = s3client.listObjects(bucketNode.data.getName()).getObjectSummaries();
            String nowPath = objectNode.data.getKey();
            for (S3ObjectSummary i : obj) {
                String key = i.getKey();
                if (key.startsWith(nowPath) && !key.equals(nowPath)) {
                    objectNode.add(new ObjectNode(i, key.charAt(key.length() - 1) == '/' ? 1 : 0));
                }
            }
        }
    }
}

class MainFrame extends JFrame{
    static DataTree dataTree = new DataTree();
    MainFrame() {
        this.setTitle("xxxx");
        this.setSize(400, 300);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLayout(null);
        this.setVisible(true);
        JScrollPane js = new JScrollPane(dataTree);
        js.setSize(300, 200);
        js.setLocation(50, 30);
        this.add(js);
    }
}

public class Main {
    public static void main(String[] args) {
        MainFrame mainFrame = new MainFrame();
    }
}

