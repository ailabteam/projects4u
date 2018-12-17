
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

import javax.swing.*;
import javax.swing.filechooser.*;

public class ManageClient implements IManageClient, IText {

	private static final ConfigClient	CONFIG_INSTANCE	= ConfigClient.getInstance();

	private ClientUI					clientUI;
	private LazySocket					lazySocketRoom;
	private LazySocket					lazySocketMainForListen;

	private String						hostMainServer;
	private int							portMainServer;
	private boolean						isInnerRoom;

	private ChatUser					user;
	private JButton						btnJoin;
	private JButton						btnLeave;
	private JButton						btnLoadMore;
	private JButton						btnSendChat;
	private JTextArea					txtContentChat;
	private JTextField					txtRoomId;
	private JTextField					txtSendChat;
	private JButton						btnSendFile;
	private JFileChooser				chooser;

	private StringBuilder				sb;

	private int							curNumOfContent;

	public ManageClient(ChatUser user, LazySocket lazySocketMainForListen) {

		super();
		this.user = user;
		this.lazySocketMainForListen = lazySocketMainForListen;
		try {
			initialize();
			addEvents();
			setFlagButton(false);
			listenFromMain(this.lazySocketMainForListen);
		} catch(IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	@Override
	public boolean gotoRoom(CustomAddress addressRoomServer) throws IOException, ClassNotFoundException {

		System.out.println("ManageClient.gotoRoom()");

		String hostRoom = addressRoomServer.getHost();
		int portRoom = addressRoomServer.getPort();

		/* Debug */
		System.out.println(hostRoom + "->" + portRoom);

		Socket socket = new Socket(hostRoom, portRoom);
		lazySocketRoom = new LazySocket(socket);

		ObjectOutputStream oos = lazySocketRoom.getOos();
		oos.writeUTF("gotoRoom");
		oos.flush();

		System.out.println("user: " + user);
		oos.writeObject(user);
		oos.flush();

		ObjectInputStream ois = lazySocketRoom.getOis();
		String result = ois.readUTF();

		return result.equalsIgnoreCase("success");
	}

	@Override
	public synchronized void leaveRoom() throws IOException {

		System.out.println("ManageClient.leaveRoom()");
		ObjectOutputStream oos = lazySocketRoom.getOos();
		oos.writeUTF("leaveRoom");
		oos.flush();

		curNumOfContent = 0;
		isInnerRoom = false;
		if(lazySocketRoom != null) {
			lazySocketRoom.cleanup();
			lazySocketRoom = null;
		}
		setFlagButton(false);
	}

	@Override
	public CustomAddress receiceAddressRoomFromMainServer(LazySocket lazySocketMainServer) throws IOException, ClassNotFoundException {

		System.out.println("lazySocketMainServer");
		System.out.println(lazySocketMainServer.getSocket().getRemoteSocketAddress());

		ObjectInputStream ois = lazySocketMainServer.getOis();
		CustomAddress address = (CustomAddress)ois.readObject();

		System.out.println("---");
		System.out.println(address);
		return address;
	}

	@SuppressWarnings("unchecked")
	@Override
	public ArrayList<Message> receiveHistory(LazySocket lazySocket) throws IOException, ClassNotFoundException {

		System.out.println("ManageClient.receiveHistory()");
		ObjectInputStream ois = lazySocket.getOis();
		if(ois.readUTF().equalsIgnoreCase("history")) { return (ArrayList<Message>)ois.readObject(); }
		return null;
	}

	@Override
	public synchronized void reiceiveChatFromRoom() {

		System.out.println("ManageClient.reiceiveChatFromRoom()");

		new Thread(new Runnable() {

			@Override
			public synchronized void run() {

				try {
					while(isInnerRoom) {
						ObjectInputStream ois = lazySocketRoom.getOis();
						String key = ois.readUTF().toLowerCase();
						if("chat".equalsIgnoreCase(key)) {
							Message message = (Message)ois.readObject();
							txtContentChat.append(message.toString());
							txtContentChat.setCaretPosition(txtContentChat.getText().length());
						} else if("loadmore".equalsIgnoreCase(key)) {
							loadHistory();
						} else if("kick".equalsIgnoreCase(key)) {
							xuLyBiDuoiKhoiPhong();
						}
					}
				} catch(EOFException e) {
					xuLyThoatKhoiPhong();
					IText.showText(LOST_CONNECT, ERROR_MESSAGE);
					setFlagButton(false);
				} catch(SocketException e) {
					if(e.getMessage().equalsIgnoreCase("Connection reset")) {
						IText.showText(LOST_CONNECT, ERROR_MESSAGE);
					}
					xuLyThoatKhoiPhong();

				} catch(IOException e) {
					e.printStackTrace();
				} catch(ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
		}).start();

	}

	@Override
	public CustomAddress requestMainServer(String id) throws IOException, ClassNotFoundException, InterruptedException {

		System.out.println("ManageClient.requestMainServer()");
		Socket socket = new Socket(hostMainServer, portMainServer);

		System.out.println(hostMainServer + "-> config client:" + portMainServer);

		LazySocket lazySocketMainServer = new LazySocket(socket);

		ObjectOutputStream oos = lazySocketMainServer.getOos();
		oos.writeUTF("user");
		oos.writeUTF(id);
		oos.flush();

		return receiceAddressRoomFromMainServer(lazySocketMainServer);

	}

	@Override
	public synchronized void sendChatToRoomServer(Message message) throws IOException {

		curNumOfContent++;
		ObjectOutputStream oos = lazySocketRoom.getOos();
		oos.writeUTF("chat");
		oos.flush();
		oos.writeObject(message);
		oos.flush();
	}

	protected synchronized void xuLyBiDuoiKhoiPhong() throws IOException {

		IText.showText(KICKED_OUT, JOptionPane.WARNING_MESSAGE);
		leaveRoom();
		setFlagButton(false);
	}

	protected synchronized void xuLyLeaveRoom() {

		System.out.println("ManageClient.xuLyLeaveRoom()");
		try {
			leaveRoom();
		} catch(IOException e) {
			e.printStackTrace();
		} finally {
			setFlagButton(false);
		}
	}

	protected synchronized void xuLyLoadMore() {

		System.out.println("ManageClient.xuLyLoadMore()");
		try {
			btnLoadMore.setEnabled(false);
			loadMore();
		} catch(ClassNotFoundException | IOException e) {
			e.printStackTrace();
		} finally {
			try {
				Thread.sleep(500);
			} catch(InterruptedException e) {
				e.printStackTrace();
			}
			btnLoadMore.setEnabled(true);
		}

	}

	protected synchronized void xuLySendChat() {

		try {
			String msg = URLEncoder.encode(txtSendChat.getText(), "UTF-8");

			msg = pretreatment(msg);// Tien xu ly chuoi

			if(!checkString(msg)) { return; }

			Message message = new Message(user, msg, Calendar.getInstance().getTime());
			sendChatToRoomServer(message);
			txtSendChat.setText("");
			txtSendChat.requestFocus();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

	protected synchronized void xuLySendFile() throws IOException {

		System.out.println("ManageClient.xuLySendFile()");
		File file = null;
		if(chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
			file = chooser.getSelectedFile();
		}
		if((file == null) || !file.exists()) {
			IText.showText("File or folder does not exist", ERROR_MESSAGE);
			return;
		}

		ObjectOutputStream oos = lazySocketRoom.getOos();
		oos.writeUTF("sendfile");
		oos.flush();
		oos.writeUTF(file.getName());
		oos.flush();

		// transfer here, one row, that is feature :)
		Files.copy(file.toPath(), oos);
		oos.flush();

	}

	protected synchronized void xuLyThoatKhoiPhong() {

		isInnerRoom = false;
		setFlagButton(isInnerRoom);
		if(lazySocketRoom != null) {
			lazySocketRoom.cleanup();
		}
		lazySocketRoom = null;
	}

	protected synchronized void xuLyThoatUI() {

		int ret = IText.showConfirm("Exit ?", JOptionPane.YES_NO_OPTION);
		if(ret == YES_OPTION) {
			try {
				Socket socket = new Socket(hostMainServer, portMainServer);
				LazySocket lazySocketMain = new LazySocket(socket);

				ObjectOutputStream oos = lazySocketMain.getOos();
				oos.writeUTF("logout");
				oos.writeUTF(user.getUsername());
				oos.flush();

				lazySocketMain.cleanup();

				cleanup();
			} catch(IOException e) {
				e.printStackTrace();
			}
			System.exit(0);
		}

	}

	protected synchronized void xuLyVaoPhong() {

		System.out.println("ManageClient.xuLyVaoPhong()");
		String idRoom = txtRoomId.getText();
		if(!checkString(idRoom)) { return; }
		System.out.println("request: " + idRoom);
		CustomAddress addressRoomServer;
		try {
			addressRoomServer = requestMainServer(idRoom);
		} catch(Exception e) {
			IText.showText(CAN_NOT_CONNECT, ERROR_MESSAGE);// Main server
			return;
		}

		if(addressRoomServer == null) {
			IText.showText(ROOM_IS_NOT_AVAILABLE, WARNING_MESSAGE);// Room server
			return;
		}

		try {
			if(gotoRoom(addressRoomServer)) {
				setFlagButton(true);
				isInnerRoom = true;
				loadHistory();
				reiceiveChatFromRoom();
			} else {
				IText.showText(ROOM_FULL, ERROR_MESSAGE);
			}
		} catch(ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}

	}

	private synchronized void addEvents() {

		btnJoin.addActionListener(new ActionListener() {

			@Override
			public synchronized void actionPerformed(ActionEvent e) {

				xuLyVaoPhong();
			}
		});
		btnLeave.addActionListener(new ActionListener() {

			@Override
			public synchronized void actionPerformed(ActionEvent e) {

				xuLyLeaveRoom();

			}
		});
		btnLoadMore.addActionListener(new ActionListener() {

			@Override
			public synchronized void actionPerformed(ActionEvent e) {

				xuLyLoadMore();

			}
		});

		btnSendChat.addActionListener(new ActionListener() {

			@Override
			public synchronized void actionPerformed(ActionEvent e) {

				xuLySendChat();

			}
		});
		clientUI.addWindowListener(new WindowAdapter() {

			@Override
			public synchronized void windowClosing(WindowEvent e) {

				xuLyThoatUI();
			}
		});
		txtSendChat.addKeyListener(new KeyAdapter() {

			@Override
			public synchronized void keyPressed(KeyEvent e) {

				if(e.getKeyCode() == KeyEvent.VK_ENTER) {
					btnSendChat.doClick();
				}
			}
		});
		txtRoomId.addKeyListener(new KeyAdapter() {

			@Override
			public synchronized void keyPressed(KeyEvent e) {

				if(e.getKeyCode() == KeyEvent.VK_ENTER) {
					btnJoin.doClick();
				}
			}
		});
		btnSendFile.addActionListener(new ActionListener() {

			@Override
			public synchronized void actionPerformed(ActionEvent e) {

				try {
					xuLySendFile();
				} catch(IOException e1) {
					e1.printStackTrace();
				}

			}
		});

	}

	private boolean checkString(String idRoom) {

		if((idRoom == null) || idRoom.isEmpty()) { return false; }
		return true;
	}

	private synchronized void cleanup() {

		System.out.println("ManageClient.cleanup()");
		if(lazySocketMainForListen != null) {
			lazySocketMainForListen.cleanup();
		}
		if(lazySocketRoom != null) {
			lazySocketRoom.cleanup();
		}
	}

	private synchronized void initialize() throws IOException {

		hostMainServer = CONFIG_INSTANCE.getHostMainServer();
		portMainServer = CONFIG_INSTANCE.getPortMainServer();

		clientUI = ClientUI.getInstance();

		btnJoin = clientUI.getBtnJoin();
		btnLeave = clientUI.getBtnLeave();
		btnLoadMore = clientUI.getBtnLoadMore();
		btnSendChat = clientUI.getBtnSendChat();

		btnSendFile = clientUI.getBtnSendFile();

		txtContentChat = clientUI.getTxtContentChat();
		txtRoomId = clientUI.getTxtRoomId();
		txtSendChat = clientUI.getTxtSendChat();

		chooser = new JFileChooser();
		chooser.addChoosableFileFilter(new FileNameExtensionFilter("Images", "jpg", "png", "gif", "bmp"));
		chooser.addChoosableFileFilter(new FileNameExtensionFilter("File", "pdf", "doc", "docx", "xlsx"));
		chooser.addChoosableFileFilter(new FileNameExtensionFilter("Zip", "zip", "rar5"));

		sb = new StringBuilder();
		curNumOfContent = 0;
	}

	private synchronized void listenFromMain(LazySocket lazySocket) {

		// TODO
		new Thread(() -> {
			ObjectInputStream ois = lazySocket.getOis();
			try {
				while(true) {
					String s = ois.readUTF();
					if(s.equalsIgnoreCase("disconnected")) {
						IText.showText(OTHER_PEOPLE_ARE_TRYING_TO_SIGN_IN_TO_THIS_ACCOUNT, ERROR_MESSAGE);

						clientUI.dispose();

						LoginUI ui = LoginUI.getInstance();
						ui.showWindow();

					}
				}
			} catch(IOException e) {
				System.out.println(DISCONNECTED_FROM_MAIN_SERVER);
				IText.showText(DISCONNECTED_FROM_MAIN_SERVER, ERROR_MESSAGE);
				cleanup();

				clientUI.dispose();

				LoginUI ui = LoginUI.getInstance();
				ui.showWindow();
			}

		}).start();
	}

	private synchronized void loadHistory() throws IOException, ClassNotFoundException {

		sb.setLength(0);

		ArrayList<Message> history = receiveHistory(lazySocketRoom);
		curNumOfContent += history.size();

		for(Message m : history) {
			sb.append(m.toString());
		}

		String text = txtContentChat.getText();
		sb.append(text);
		txtContentChat.setText(URLDecoder.decode(sb.toString(), "UTF-8"));
	}

	private synchronized void loadMore() throws IOException, ClassNotFoundException {

		System.out.println("ManageClient.loadMore()");
		ObjectOutputStream oos = lazySocketRoom.getOos();
		oos.writeUTF("loadMore");
		oos.flush();
		oos.writeUTF(String.valueOf(curNumOfContent));
		oos.flush();

	}

	private String pretreatment(String msg) {

		// TODO
		// xoa khoang trang
//		return msg.replaceAll("\\s ", "").trim();// remove space excess
		return msg;
	}

	private synchronized void setFlagButton(boolean b) {

		txtContentChat.setText("");
		txtRoomId.setEnabled(!b);
		btnJoin.setEnabled(!b);
		btnLeave.setEnabled(b);
		btnLoadMore.setEnabled(b);
		btnSendChat.setEnabled(b);
		btnSendFile.setEnabled(b);
	}

}
