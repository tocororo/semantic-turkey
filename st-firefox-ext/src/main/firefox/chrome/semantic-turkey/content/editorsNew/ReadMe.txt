When double click on a class the following request is sent to the server:

http://127.0.0.1:1979/semantic_turkey/resources/stserver/STServer?service=cls&request=getClsDescription&clsName=http%3A%2F%2Ftest%23ClassePadre&method=templateandvalued

http://127.0.0.1:1979/semantic_turkey/resources/stserver/STServer?service=annotation&request=getAnnotatedContentResources&resource=http%3A%2F%2Ftest%23ClassePadre

http://127.0.0.1:1979/semantic_turkey/resources/stserver/STServer?service=cls&request=getClassAndInstancesInfo&clsName=http%3A%2F%2Ftest%23ClassePadre


When double clicking on an instance the following request is sent to the server:

http://127.0.0.1:1979/semantic_turkey/resources/stserver/STServer?service=individual&request=getIndDescription&instanceQName=andrea&method=templateandvalued