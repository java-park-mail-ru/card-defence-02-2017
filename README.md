# tower-defence-02-2017
KVTeam: Каширин Максим, Виноградов Андрей

<p style="font-size:40;">API:</p><br><br>
<p>Каждый запрос может вернуть 400 код ответа в случае,
если отправленные в запросе данные не соответствуют
указанным в документации. В теле ответа будет JSON
{"code": 400, "message": "invalid request"} </p>

<h4 style="">POST /api/account</h4> <br>
<p>Регистрация пользователя с последующим логином</p>
<p>Запрос должен содержать поля:</p>
<p style="margin-left: 40px;">{"username" : ..., "password": ..., "email":...}</p>
<p>Результатом запроса от бекенда будет:</p>
<ol>
     <li>200 - зареган успешно</li>
     <li>403 - маловероятный кейс(зареган, но не залогинился)</li>
     <li>409 - такой никнейм есть</li>
     <li>400 - ошибка во входных данных</li>
</ol>
<p>Формат ответа при 200 коде:<p>
<p style="margin-left: 40px;">{"username" : ..., "sessionID": ...}</p>
<p>Формат ответа при 403, 409 коде:<p>
<p style="margin-left: 40px;">{"code" : ..., "message": ...}</p>

<h4 style="">POST /api/login</h4> 
<p>Логин пользователя</p>
<p>Запрос должен быть вида:</p>
<p style="margin-left: 40px;">{"username" : ..., "password": ...}</p>
<p>Результатом запроса от бекенда будет:</p>
<ol>
      <li>200 - логин успешен</li>
      <li>403 - доступ запрещен</li>
</ol>
</p>Формат ответа при 200 коде:</pi>
<p style="margin-left: 40px;">{"username" : ..., "sessionID": ...}</p>
</p>Формат ответа при 403 коде:</pi>
<p style="margin-left: 40px;">{"code" : ..., "message": ...}</p>

<h4 style="">POST /api/logout</h4>
<p>Логаут пользователя</p>
<p>Запрос должен быть вида:</p>
<p style="margin-left: 40px;">{"username" : ..., "sessionID": ...}</p>
<p>Результатом запроса от бекенда будет:</p>
<ol>
      <li>200 - гарантировано, что по указанным данным не будет залогиненного пользователя</li>
</ol>
<p style="margin-left: 40px;"> {"code" : 200, "message": "success"} </p>

<h4 style="">POST /api/isloggedin</h4>
<p>Проверка, залогинен ли пользователь</p>
<p>Запрос должен быть вида:</p>
<p style="margin-left: 40px;">{"username" : ..., "sessionID": ...}</p>
<p>Результатом запроса от бекенда будет:</p>
<ol> 
     <li>200 - сессия существует</li>
     <li>403 - нет юзера или сессия не существуют</li>
</ol>
</p>Формат ответа при 200 коде:</p>
<p style="margin-left: 40px;">{"code" : 200, "message": "success"}</p>
<p>Формат ответа при 403 коде:</p>
<p style="margin-left: 40px;">{"code" : 403, "message": "Access denied"}</p>


<h4 style="">PUT /api/account</h4>
<p>Изменение данных пользователя</p>
<p>Запрос должен быть вида:</p>
<p style="margin-left: 40px;">{"username" : ..., "sessionID": ..., "email": (необязательный), "password": (необязательный)}</p>
<p>Результатом запроса от бекенда будет:</p>
<ol> 
     <li>200 - данные изменены</li>
     <li>403 - запрещено менять данные(сессия не сошлась)</li>
</ol>
<p>Формат ответа при 403 коде:</p>
<p style="margin-left: 40px;">{"code" : ..., "message": ...}</p>


<h4 style="">GET /api/account?username=...</h4>
<p>Получение данных о пользователе</p>
<p>Результатом запроса от бекенда будет:</p>
<ol> 
     <li>200 - пользователь найден</li>
     <li>404 - пользователь не найден</li>
</ol>
<p>Формат ответа при 200 коде:</p>
<p style="margin-left: 40px;">{"username" : ..., "email":...}</p>
<p>Формат ответа при 404 коде:</p>
<p style="margin-left: 40px;">{"code" : 404, "message":"not found"}</p>
