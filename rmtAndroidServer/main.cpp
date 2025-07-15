#include <QCoreApplication>

#include "httpBackendServer.h"
#include "mysqlhandler.h"
#include "rdtserver.h"

int main(int argc, char *argv[])
{
    QCoreApplication a(argc, argv);

    MySQLHandler::instance();

    HttpBackendServer httpServer;
    RDTServer rdtServer;

    return a.exec();
}
