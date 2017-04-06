package gui;

import crawler.CrawlConfig;
import crawler.SimpleController;
import crawler.WebCrawler;
import fetcher.PageFetcher;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class crawlerFrame extends JFrame {
    private static JTextField name;
    private static JTextField depth;
    private static JComboBox DataStructure;
    private static JTextField politeDelay;
    private static JCheckBox resume;
    public static String[] parameters = new String[4]; //parameters for the crawlers (name, depth, DataStructure)
    static SimpleController controller;



    static JPanel inputPanel;
    static JPanel CountPanel;
    static JPanel outputPanel;

    static void CreateInputPanel(){
        inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.X_AXIS));
        inputPanel.add(Box.createHorizontalStrut(10));
        JLabel lbname = new JLabel("Character:");
        lbname.setAlignmentY(Component.CENTER_ALIGNMENT);
       // lbname.setBorder(BorderFactory.createEmptyBorder(30, 5, 0, 5));
        inputPanel.add(lbname);
        name = new JTextField("Yoda", 10);
        name.setEditable(true);
        JPanel NamePanel = new JPanel();
        NamePanel.setLayout(new BoxLayout(NamePanel, BoxLayout.X_AXIS));
        NamePanel.add(lbname);
        NamePanel.add(name);
        NamePanel.setAlignmentY(Component.CENTER_ALIGNMENT);
        NamePanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        inputPanel.add(NamePanel);

        JLabel lbdepth = new JLabel("Depth:");
        lbdepth.setAlignmentY(Component.CENTER_ALIGNMENT);
        //lbdepth.setBorder(BorderFactory.createEmptyBorder(30, 5, 0, 5));
        inputPanel.add(lbdepth);
        depth = new JTextField("1", 10);
        depth.setEditable(true);
        JPanel DepthPanel = new JPanel();
        DepthPanel.setLayout(new BoxLayout(DepthPanel, BoxLayout.X_AXIS));
        DepthPanel.add(lbdepth);
        DepthPanel.add(depth);
        DepthPanel.setAlignmentY(Component.CENTER_ALIGNMENT);
        DepthPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        inputPanel.add(DepthPanel);

        JLabel politessDelay = new JLabel("Politeness Delay:");
        politessDelay.setAlignmentY(Component.CENTER_ALIGNMENT);
        //inputPanel.add(politessDelay);
        politeDelay = new JTextField("200",10);
        politeDelay.setEditable(true);
        JPanel delayPanel = new JPanel();
        delayPanel.setLayout(new BoxLayout(delayPanel,BoxLayout.LINE_AXIS));
        delayPanel.add(politessDelay);
        delayPanel.add(politeDelay);
        delayPanel.setAlignmentY(Component.CENTER_ALIGNMENT);
        delayPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        inputPanel.add(delayPanel);



        JLabel lbData = new JLabel("Queue:");
        lbData.setAlignmentY(Component.CENTER_ALIGNMENT);
        //lbData.setBorder(BorderFactory.createEmptyBorder(30, 5, 0, 5));
        String[] queueType = {"BlockingQueue","LockFreeQueue","Java-BlockingQueue","Java-ConcurrentQueue"};
        DataStructure = new JComboBox(queueType);
        DataStructure.setSelectedIndex(3);
        DataStructure.setAlignmentY(Component.CENTER_ALIGNMENT);
        //DataStructure.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        JPanel DataStructurePanel = new JPanel();
        DataStructurePanel.setLayout(new BoxLayout(DataStructurePanel, BoxLayout.X_AXIS));
        DataStructurePanel.add(lbData);
        DataStructurePanel.add(DataStructure);
        DataStructurePanel.setAlignmentY(Component.CENTER_ALIGNMENT);
        DataStructurePanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        inputPanel.add(DataStructurePanel);

        /*
        DefaultListModel listmodel = new DefaultListModel();
        listmodel.addElement("BlockingQueue");
        listmodel.addElement("LockFreeQueue");
        listmodel.addElement("Java-BlockingQueue");
        listmodel.addElement("Java-ConcurrentQueue");
        DataStructure = new JList(listmodel);
        DataStructure.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        DataStructure.setVisibleRowCount(3);
        JScrollPane DataStructureScroller = new JScrollPane(DataStructure);
        DataStructureScroller.setPreferredSize(new Dimension(220, 80));
        DataStructureScroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        DataStructureScroller.setAlignmentY(Component.CENTER_ALIGNMENT);
        JPanel DataStructurePanel = new JPanel();
        DataStructurePanel.setLayout(new BoxLayout(DataStructurePanel, BoxLayout.X_AXIS));
        DataStructurePanel.add(lbData);
        DataStructurePanel.add(DataStructureScroller);
        DataStructurePanel.setAlignmentY(Component.CENTER_ALIGNMENT);
        DataStructurePanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        inputPanel.add(DataStructurePanel);

        inputPanel.add(Box.createHorizontalStrut(10));*/
    }

    static void CreateCountPanel(){
        CountPanel = new JPanel();
        CountPanel.setLayout(new BoxLayout(CountPanel, BoxLayout.X_AXIS));
        JTextArea namelist = new JTextArea(120,80);
        JButton start = new JButton("Start");
        JButton stop = new JButton("Stop");
        resume = new JCheckBox("Resume");
        resume.setSelected(false);

        JPanel ButtonPanel = new JPanel();
        ButtonPanel.setLayout(new BoxLayout(ButtonPanel, BoxLayout.Y_AXIS));
        ButtonPanel.add(resume);
        ButtonPanel.add(start);
        ButtonPanel.add(stop);
        ButtonPanel.setBorder(BorderFactory.createEmptyBorder(200, 15, 15, 15));
        CountPanel.add(ButtonPanel);

        JLabel lbresult = new JLabel("Characters: ");
        lbresult.setAlignmentY(Component.CENTER_ALIGNMENT);
        lbresult.setBorder(BorderFactory.createEmptyBorder(300, 5, 0, 5));

        namelist.setEditable(false);
        namelist.setLineWrap(true);
        JScrollPane namelistScroller = new JScrollPane(namelist);
        JPanel resultPanel = new JPanel();
        resultPanel.setLayout(new BoxLayout(resultPanel, BoxLayout.X_AXIS));
        resultPanel.add(lbresult);
        resultPanel.add(namelistScroller);
        resultPanel.setAlignmentY(Component.TOP_ALIGNMENT);
        resultPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 3));
        CountPanel.add(resultPanel);

        start.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent evt){
                parameters[0] = name.getText();         //character
                parameters[1] = depth.getText();         //depth
                parameters[2] = politeDelay.getText();   //politeDelay
                parameters[3] = (String)DataStructure.getSelectedItem();       //DataStructure choosed
               // parameters[4] = resume.


                //isStart = true;
                String folder = "./data/";
                int numofCrawlers = Runtime.getRuntime().availableProcessors();
                int maxDepth = Integer.parseInt(parameters[1]);
                int politeDelay = Integer.parseInt(parameters[2]);

                namelist.append("Crawler process start...\n\r");
                namelist.append("Using "+numofCrawlers+" threads...\n\r");


                CrawlConfig config = new CrawlConfig();
                config.setCrawlStorageFolder(folder);
                config.setMaxDepthOfCrawling(maxDepth);
                config.setPolitenessDelay(politeDelay);
                config.setResumable(resume.isSelected());

                PageFetcher pageFetcher = new PageFetcher(config);
                controller = new SimpleController(config, pageFetcher, parameters[3]);
                if(!resume.isSelected()) {
                    controller.addSeed("http://starwars.wikia.com/wiki/" + parameters[0]);
                }
                controller.start(WebCrawler.class, numofCrawlers);
            }
        });

        stop.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                controller.shutdown();
                namelist.append("Process stopped ");
            }
        });

    }

    static void CreateOutputPanel(){
        outputPanel = new JPanel();
        outputPanel.setLayout(new BoxLayout(outputPanel, BoxLayout.X_AXIS));
        outputPanel.add(Box.createVerticalStrut(10));
        outputPanel.add(Box.createHorizontalStrut(90));
        JLabel lbrt = new JLabel("Running Time: ");
        lbrt.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        JTextField rt = new JTextField("ms", 10);
        rt.setEditable(false);
        outputPanel.add(lbrt);
        outputPanel.add(rt);
        outputPanel.add(Box.createHorizontalStrut(50));
        outputPanel.add(Box.createVerticalStrut(10));
    }

    public static void main(String args[]){
        CreateInputPanel();
        CreateCountPanel();
        CreateOutputPanel();
        JPanel panelContainer = new JPanel();
        panelContainer.setLayout(new GridBagLayout());
        GridBagConstraints c1 = new GridBagConstraints();
        c1.gridx = 0;
        c1.gridy = 0;
        c1.weightx = 1.0;
        c1.weighty = 1.0;
        c1.fill = GridBagConstraints.HORIZONTAL;
        panelContainer.add(inputPanel, c1);

        GridBagConstraints c2 = new GridBagConstraints();
        c2.gridx = 0;
        c2.gridy = 1;
        c2.weightx = 1.0;
        c2.weighty = 1.0;
        c2.fill = GridBagConstraints.BOTH;
        panelContainer.add(CountPanel, c2);

        /*GridBagConstraints c3 = new GridBagConstraints();
        c3.gridx = 0;
        c3.gridy = 2;
        c3.weightx = 1.0;
        c3.weighty = 0;
        c3.fill = GridBagConstraints.HORIZONTAL;
        panelContainer.add(outputPanel, c3);*/

        JFrame SWFrame = new JFrame("Star War Character Search");
        SWFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        panelContainer.setOpaque(true);
        SWFrame.setSize(new Dimension(700, 600));
        SWFrame.setContentPane(panelContainer);
        SWFrame.setVisible(true);





    }


}