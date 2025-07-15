#ifndef PERMISSION_H
#define PERMISSION_H

#include <QByteArray>

class QJsonObject;

class Permission
{
public:
    enum PermissionInfo
    {
        Screen,
        Microphone,
        Camera,
        RemoteInput,
        AccessFile,
        PermissionInfoCount
    };

    Permission();

    bool    isAllowed(PermissionInfo info) const;
    void    setAllow(PermissionInfo info, bool value);

    QString toJSON();
    void    fromJSON(const QJsonObject &);

private:

    bool    _permission[PermissionInfoCount];
};

#endif // PERMISSION_H
