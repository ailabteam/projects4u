
import java.awt.*;

import javax.swing.*;

public interface IText {

	int		DEFAULT_OPTION										= -1;
	int		YES_NO_OPTION										= 0;
	int		YES_NO_CANCEL_OPTION								= 1;
	int		OK_CANCEL_OPTION									= 2;
	int		YES_OPTION											= 0;
	int		NO_OPTION											= 1;
	int		CANCEL_OPTION										= 2;
	int		OK_OPTION											= 0;
	int		ERROR_MESSAGE										= 0;
	int		INFORMATION_MESSAGE									= 1;
	int		WARNING_MESSAGE										= 2;
	int		QUESTION_MESSAGE									= 3;
	int		PLAIN_MESSAGE										= -1;
	String	ICON_PROPERTY										= "icon";
	String	MESSAGE_PROPERTY									= "message";
	String	VALUE_PROPERTY										= "value";
	String	OPTIONS_PROPERTY									= "options";
	String	INITIAL_VALUE_PROPERTY								= "initialValue";
	String	MESSAGE_TYPE_PROPERTY								= "messageType";
	String	OPTION_TYPE_PROPERTY								= "optionType";
	String	SELECTION_VALUES_PROPERTY							= "selectionValues";
	String	INITIAL_SELECTION_VALUE_PROPERTY					= "initialSelectionValue";
	String	INPUT_VALUE_PROPERTY								= "inputValue";
	String	WANTS_INPUT_PROPERTY								= "wantsInput";

	String	CAN_NOT_CONNECT										= "Can not connect to server!";
	String	KICKED_OUT											= "You were kicked out of room!";
	String	LOST_CONNECT										= "You lost the connection!";
	String	ROOM_FULL											= "Room was full!";
	String	ROOM_IS_NOT_AVAILABLE								= "Room is not available. Try again later!";
	String	OTHER_PEOPLE_ARE_TRYING_TO_SIGN_IN_TO_THIS_ACCOUNT	= "Other people are trying to sign in to this account";
	String	DISCONNECTED_FROM_MAIN_SERVER						= "disconnected from main server!!";

	public static int showConfirm(Object message, int opitonType) {

		return JOptionPane.showConfirmDialog(null, message, "Confirm", opitonType);

	}

	public static String showInput(Component message, Object initialSelectionValue) {

		return JOptionPane.showInputDialog(message, initialSelectionValue);
	}

	public static void showText(Object message, int messageType) {

		JOptionPane.showMessageDialog(null, message, "Notice", messageType);

	}

}
