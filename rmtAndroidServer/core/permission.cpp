#include "permission.h"

#include <QJsonDocument>
#include <QJsonObject>

const char * gPermissionKeys[] = {"screen", "microphone", "camera", "remote_input", "file_access"};

Permission::Permission() {
    for (int i = 0; i < PermissionInfoCount; i++)
        _permission[i] = false;
}

bool Permission::isAllowed(PermissionInfo info) const
{
    return _permission[info];
}

void Permission::setAllow(PermissionInfo info, bool v)
{
    _permission[info] = v;
}

QString Permission::toJSON()
{
    QJsonObject json;
    for (int i = 0; i < PermissionInfoCount; i++)
    {
        const char *key = gPermissionKeys[i];
        json[key] = _permission[i] ? 1 : 0;
    }

    QJsonDocument jsonDoc(json);
    QByteArray jsonByte = jsonDoc.toJson(QJsonDocument::JsonFormat::Compact);
    return QString::fromUtf8(jsonByte);
}

void Permission::fromJSON(const QJsonObject & jsonObject)
{
    for (int i = 0; i < PermissionInfoCount; i++) {
        const char *key = gPermissionKeys[i];

        // Ensure the key exists in the permissions object
        if (jsonObject.contains(key)) {
            bool value = jsonObject[key].toInt(); // Access the value
            _permission[i] = value; // Store the value
        } else {
            _permission[i] = false; // Default or handle missing keys
        }
    }
}
