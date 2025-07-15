#ifndef MYSQLHANDLER_H
#define MYSQLHANDLER_H

#include <QObject>
#include <QString>

#include <QSqlDatabase>

class User;
class Permission;
class MySQLHandler : public QObject
{
    Q_OBJECT
public:
    static  MySQLHandler *  instance();

    int     singup(const User & user);
    int     login(User * user);
    int     resetPassword(const QString & phonenumber, const QString & newPassword);
    int     updatePassword(const QString & phonenumber, const QString & curPassword, const QString & newPassword);

    void    getPermission(const QString & phonenumber, Permission & permission);
    int     setPermission(const QString & phonenumber, const Permission & permission);

    void    loadAllUsers(QList<User> & allUsers);

signals:

private:
    MySQLHandler(QObject *parent = nullptr);

    void                    pingQuery();

    static MySQLHandler *   m_instance;

    QSqlDatabase            m_db;
};

#endif // MYSQLHANDLER_H
