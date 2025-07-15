#ifndef BACKENDDEFINE_H
#define BACKENDDEFINE_H

#define BACKEND_PORT        5558

#define DEBUG_MSG_BACKEND_STARTED_PORT  "Backend Server started on port"
#define DEBUG_MSG_BACKEND_FAILED        "Failed to start Backend server"

#define API_WELCOME                     "/api/welcome"
#define API_REGISTER                    "/api/register"
#define API_LOGIN                       "/api/login"
#define API_RESET_PASSWORD              "/api/resetpassword"
#define API_UPDATE_PASSWORD             "/api/updatepassword"
#define API_GET_PERMISSIONS             "/api/getpermissions"
#define API_SET_PERMISSIONS             "/api/setpermissions"

#define API_ALL                         {API_WELCOME, API_REGISTER, API_LOGIN, API_RESET_PASSWORD, API_UPDATE_PASSWORD, API_GET_PERMISSIONS, API_SET_PERMISSIONS}

enum API_INFO
{
    API_INFO_UNKNOWN = -1,
    API_INFO_WELCOME,
    API_INFO_REGISTER,
    API_INFO_LOGIN,
    API_INFO_RESET_PASSWORD,
    API_INFO_UPDATE_PASSWORD,
    API_INFO_GET_PERMISSIONS,
    API_INFO_SET_PERMISSIONS,
    API_INFO_COUNT
};

#define MSG_INPUT_INCORRECT            "The input data is incorrect."
#define MSG_INPUT_INCORRECT_CH         "输入的数据不正确."

#define MSG_SIGNUP_SUCCESS             "Successfully Registered!"
#define MSG_SIGNUP_SUCCESS_CH          "注册成功！"

#define MSG_SIGNUP_FAILED_REASON       {"Registered Phone Number", "Unknown Error"}
#define MSG_SIGNUP_FAILED_REASON_CH    {"已注册的电话号码", "未知错误"}

#define MSG_LOGIN_SUCCESS             "Successfully logged in!"
#define MSG_LOGIN_SUCCESS_CH          "登录成功！"

#define MSG_LOGIN_FAILED_REASON       {"Unregistered Phone Number", "Incorrect Password", "Other Reason"}
#define MSG_LOGIN_FAILED_REASON_CH    {"未注册的电话号码", "密码不正确", "其他原因"}

#define MSG_RESET_PWD_SUCCESS        "password has been changed successfully!"
#define MSG_RESET_PWD_SUCCESS_CH     "密码修改成功!"

#define MSG_OPERATION_FAILED          "operation failed"
#define MSG_OPERATION_FAILED_CH       "操作失败!"

#define MSG_UPDATE_PWD_FAILED_REASON       {"Incorrect Current Password", "Other Reason"}
#define MSG_UPDATE_PWD_FAILED_REASON_CH    {"当前密码不正确", "其他原因"}

#endif // BACKENDDEFINE_H
