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
        System.out.println(s);
        int tmp = s.length() - nodeType;
        int t = s.lastIndexOf('/', tmp - 1);
        System.out.println(t + " " + tmp);
        return s.substring(t + 1, tmp);
    }
}

class DataTree extends JTree implements TreeWillExpandListener{
    AWSCredentials myCredentials = new BasicAWSCredentials("AKIAJJFM2OICN5ORSXFQ", "dwG4N7uwByASYD/d93RB9KT8EhbyMF/gATOnGfCE");
    AmazonS3 s3client = new AmazonS3Client(myCredentials);
    DataTree() {
        this.setRootVisible(true);
        this.addTreeWillExpandListener(this);
        DefaultMutableTreeNode treeRoot = new DefaultMutableTreeNode("Buckets");
        DefaultTreeModel e3Model = new DefaultTreeModel(treeRoot);
        this.setModel(e3Model);
        List<Bucket> bucketList = s3client.listBuckets();
        for (Bucket i : bucketList) {
            treeRoot.add(new BucketNode(i));
        }
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
        this.setSize(800, 400);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLayout(null);
        this.setVisible(true);
        JScrollPane js = new JScrollPane(dataTree);
        js.setSize(300, 200);
        js.setLocation(450, 70);
        this.add(js);
    }
}

public class Main {
    public static void main(String[] args) {
        MainFrame mainFrame = new MainFrame();
    }
}

