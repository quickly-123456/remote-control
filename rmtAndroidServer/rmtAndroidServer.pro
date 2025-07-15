QT = core network gui sql websockets

CONFIG += c++17 cmdline

# You can make your code fail to compile if it uses deprecated APIs.
# In order to do so, uncomment the following line.
#DEFINES += QT_DISABLE_DEPRECATED_BEFORE=0x060000    # disables all the APIs deprecated before Qt 6.0.0

INCLUDEPATH += core model logger

SOURCES += \
        core/channel.cpp \
        core/mysqlhandler.cpp \
        core/permission.cpp \
        core/rdtmessage.cpp \
        core/rdtserver.cpp \
        core/user.cpp \
        core/usersocket.cpp \
        core/vuesocket.cpp \
        logger/logger.cpp \
        main.cpp \
        core/httpBackendServer.cpp

# Default rules for deployment.
qnx: target.path = /tmp/$${TARGET}/bin
else: unix:!android: target.path = /opt/$${TARGET}/bin
!isEmpty(target.path): INSTALLS += target

HEADERS += \
    core/appDefine.h \
    core/backendDefine.h \
    core/channel.h \
    core/httpBackendServer.h \
    core/mysqlhandler.h \
    core/permission.h \
    core/rdtDefine.h \
    core/rdtmessage.h \
    core/rdtserver.h \
    core/user.h \
    core/usersocket.h \
    core/vuesocket.h \
    logger/logger.h

LIBS += -L$$PWD/lib -llibmysql
