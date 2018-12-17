
import java.awt.*;

import javax.swing.*;

public class ClientUI extends JFrame {

	/**
	 *
	 */
	private static final long	serialVersionUID	= 1L;
	private static ClientUI		instance			= new ClientUI();
	private JTextField			txtSendChat;
	private JTextField			txtRoomId;
	private JButton				btnJoin;
	private JButton				btnLeave;
	private JButton				btnLoadMore;
	private JTextArea			txtContentChat;
	private JButton				btnSendChat;
	private JButton				btnSendFile;

	public static ClientUI getInstance() {

		return instance;
	}

	public static long getSerialversionuid() {

		return serialVersionUID;
	}

	private ClientUI() throws HeadlessException {

		super();
		addControls();
	}

	public JButton getBtnJoin() {

		return btnJoin;
	}

	public JButton getBtnLeave() {

		return btnLeave;
	}

	public JButton getBtnLoadMore() {

		return btnLoadMore;
	}

	public JButton getBtnSendChat() {

		return btnSendChat;
	}

	public JButton getBtnSendFile() {

		return btnSendFile;
	}

	public JTextArea getTxtContentChat() {

		return txtContentChat;
	}

	public JTextField getTxtRoomId() {

		return txtRoomId;
	}

	public JTextField getTxtSendChat() {

		return txtSendChat;
	}

	public void showWindow() {

		setSize(640, 460);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		setLocationRelativeTo(null);
		setVisible(true);

	}

	private void addControls() {

		Container con = getContentPane();
		con.setLayout(new BorderLayout());

		JPanel pnMain = new JPanel();
		getContentPane().add(pnMain, BorderLayout.CENTER);
		pnMain.setLayout(new BorderLayout(0, 0));

		JPanel panel = new JPanel();
		pnMain.add(panel, BorderLayout.NORTH);
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

		JLabel lblIdRoom = new JLabel("ROOM ID:");
		panel.add(lblIdRoom);

		txtRoomId = new JTextField();
		txtRoomId.setText("Room01");
		panel.add(txtRoomId);
		txtRoomId.setColumns(10);

		btnJoin = new JButton("JOIN");
		panel.add(btnJoin);

		btnLeave = new JButton("LEAVE");
		panel.add(btnLeave);

		btnLoadMore = new JButton("LOADMORE");
		panel.add(btnLoadMore);

		JPanel pnBtn = new JPanel();
		pnMain.add(pnBtn, BorderLayout.SOUTH);
		pnBtn.setLayout(new BoxLayout(pnBtn, BoxLayout.X_AXIS));

		txtSendChat = new JTextField();
		pnBtn.add(txtSendChat);
		txtSendChat.setColumns(10);

		btnSendChat = new JButton("SEND");
		pnBtn.add(btnSendChat);

		btnSendFile = new JButton("SEND FILE");
		pnBtn.add(btnSendFile);
		btnSendFile.setVisible(false);

		JPanel pnMid = new JPanel();
		pnMain.add(pnMid, BorderLayout.CENTER);
		pnMid.setLayout(new BorderLayout(0, 0));

		JScrollPane scrollPane = new JScrollPane();
		pnMid.add(scrollPane);

		txtContentChat = new JTextArea();
		txtContentChat.setWrapStyleWord(true);
		txtContentChat.setLineWrap(true);
		txtContentChat.setFont(new Font("", Font.PLAIN, 14));
		txtContentChat.setEditable(false);
		scrollPane.setViewportView(txtContentChat);

	}
}
