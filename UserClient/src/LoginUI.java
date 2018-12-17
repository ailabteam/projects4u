
import java.awt.*;
import java.awt.event.*;
import java.io.*;

import javax.swing.*;

public class LoginUI extends JFrame implements IText {

	/**
	 *
	 */
	private static final long	serialVersionUID	= 1L;

	private static LoginUI		instance			= new LoginUI("LoginUI");

	private JPasswordField		txtPassWord;
	private JButton				btnLogin;
	private JButton				btnExit;

	private JTextField			txtUserName;

	public static LoginUI getInstance() {

		return instance;
	}

	private LoginUI(String title) throws HeadlessException {

		super(title);
		addControls();
		addEvents();

		fakeData();// TODO
	}

	public void showWindow() {

		pack();
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		setLocationRelativeTo(null);
		setVisible(true);
	}

	protected void xuLyLogin() {

		// TODO Auto-generated method stub
		System.out.println("LoginUI.xuLyLogin()");

		String username = txtUserName.getText();
		String pass = String.valueOf(txtPassWord.getPassword());
		try {
			ILogin login = new Login();
			ChatUser user = login.login(username, pass);
			LazySocket lazySocketMain = login.getLazySocket();
			if(user != null) {
				new ManageClient(user, lazySocketMain);
				ClientUI ui = ClientUI.getInstance();
				ui.showWindow();
				dispose();
			} else {
				IText.showText("Login failed!", ERROR_MESSAGE);
			}
		} catch(ClassNotFoundException | IOException e) {
			IText.showText("Login failed", ERROR_MESSAGE);
			e.printStackTrace();
		}

	}

	protected void xuLyThoat() {

		int ret = IText.showConfirm("Exit ?", YES_NO_OPTION);
		if(ret == YES_OPTION) {
			System.exit(0);
		}
	}

	private void addControls() {

		int col = 20;
		Container con = getContentPane();
		con.setLayout(new BorderLayout());

		JPanel pnMain = new JPanel();
		pnMain.setLayout(new BorderLayout());
		con.add(pnMain, BorderLayout.CENTER);

		JPanel pnMid = new JPanel();
		pnMid.setLayout(new BoxLayout(pnMid, BoxLayout.Y_AXIS));
		pnMain.add(pnMid, BorderLayout.CENTER);

		JPanel pnTitle = new JPanel();
		JLabel lblTitle = new JLabel("Login");
		lblTitle.setFont(new Font("", Font.BOLD, 30));
		lblTitle.setForeground(Color.RED);
		pnTitle.add(lblTitle);
		pnMid.add(pnTitle);

		JPanel pnUserName = new JPanel();
		pnMid.add(pnUserName);
		JLabel lblUserName = new JLabel("UserName:");
		pnUserName.add(lblUserName);
		txtUserName = new JTextField(col);
		pnUserName.add(txtUserName);

		JPanel pnPassWord = new JPanel();
		pnMid.add(pnPassWord);
		JLabel lblPassWord = new JLabel("PassWord:");
		pnPassWord.add(lblPassWord);
		txtPassWord = new JPasswordField(col);
		pnPassWord.add(txtPassWord);

		JPanel pnBtn = new JPanel();
		pnMid.add(pnBtn);
		btnLogin = new JButton("Login");
		pnBtn.add(btnLogin);
		btnExit = new JButton("Exit");
		pnBtn.add(btnExit);

	}

	private void addEvents() {

		// TODO Auto-generated method stub
		addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent e) {

				xuLyThoat();
			}
		});
		btnExit.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				xuLyThoat();
			}
		});
		btnLogin.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				xuLyLogin();
			}
		});

	}

	private void fakeData() {

		txtUserName.setText("user01");
		txtPassWord.setText("123");
	}

}
