#!/usr/bin/env ol
(import (lib c!))

; ====================================================================
; подключение и настройка базы даных
; --------------------------------------------------------------------
(define *features* (cons 'sqlite-log-debug *features*)) ; enable debug
(import (lib sqlite))

(when (< (sqlite3_libversion_number) 3035005)
   (print "You'r using " (sqlite3_libversion) " Sqlite3 version,")
   (print "version 3.35.5+ is required.")
   (shutdown 1))

; включим мультипоточность
(when (or
         (zero? (sqlite3_threadsafe))
         (not (eq? (sqlite3_config SQLITE_CONFIG_MULTITHREAD) SQLITE_OK)))
   (print "You'r using not threadsafe Sqlite3 library,")
   (print "threadsafe is required.")
   (shutdown 2))

; проверим, а есть ли вообще бд?
(import (srfi 170))
(let ((stat (file-info "database.sqlite")))
   (when (or (not (file-info? stat))
             (eq? (file-info:size stat) 0))
      (print "It seems you have no database or database is empty.")
      (print "Run the ./database.lisp script to create a new one.")
      (shutdown 3)))

; simplify database access functions
; --------------------------------------------------------------------
(define-syntax db:query
   (syntax-rules (database)
      ((db:query . args)
         (sqlite:query database . args))))

(define-syntax db:value
   (syntax-rules (database)
      ((db:value . args)
         (sqlite:value database . args))))

(define db:map sqlite:map)

(define (db:apply statement f)
   (cond
      ((pair? statement)
         (apply f statement))
      (#true #false)
      (statement
         (f statement))))

(import (lang sexp))
;; (import (only (lang intern) string->uninterned-symbol))
(define (runes->number runes)
   (list->number runes 10))
;; (import (olvm syscalls))

;; (import (owl parse))


(define (api? format line handler)
   (let loop ((l (string->runes line)) (f (string->runes format)) (args #null) (types #null))
      (cond
         ((and (eq? l #null)  ; шаблон совпал
               (eq? f #null))
            (apply handler
               (map (lambda (p t)
                       ((if (eq? t #\?) runes->string runes->number)
                          (reverse
                             (let loop ((p p) (a #null))
                                (if (or (null? p) (eq? (car p) #\/))
                                   a
                                   (loop (cdr p) (cons (car p) a)))))))
                  (reverse args)
                  (reverse types)))
            #true)
         ((or (eq? l #null)  ; шаблон не совпал
              (eq? f #null))
            #false)
         ((eq? (car l) (car f))
            (loop (cdr l) (cdr f) args types))
         ((or (eq? (car f) #\?)   ; строка в шаблоне
              (eq? (car f) #\#))  ; число в шаблоне
            (let ((type (car f)))
               (let subloop ((p l))
                  (cond
                     ((null? p)
                        (loop p (cdr f) (cons l args) (cons type types)))
                     ((eq? (car p) #\/)
                        (loop p (cdr f) (cons l args) (cons type types)))
                     (else
                        (subloop (cdr p)))))))
         (else
            #false))))

(define (starts-with string sub)
   (if (> (string-length sub) (string-length string))
      #false
      (string-eq? (substring string 0 (string-length sub)) sub)))

; рабочий вариант, но много копипасты
(define-syntax REST
   (syntax-rules (else path api? args send-401
                  send-unauthorized send-forbidden send-not-implemented
                  session account url request
                  API URL GET PUT POST PATCH DELETE REQUEST)
      ((REST) #false)
      ((REST (else exp . rest))
         (begin exp . rest))        ; (begin ...)

      ((REST (API template (vars...) .body) .rest)
         (unless (api? template path
               (lambda (vars...)
                  (begin .body) ; ok?
                  (send-not-implemented)))
            (REST .rest)))
      ((REST (REQUEST request-type template (vars...) .body) .rest)
         (unless (and
               (string-eq? (ref request 1) request-type)
               (api? template path
                  (lambda (vars...)
                     .body)))
            (REST .rest)))

      ; специализации запросов
      ((REST (GET template (vars...) .body) .rest)
         (REST (REQUEST "GET" template (vars...) .body) .rest))
      ((REST (POST template (vars...) .body) .rest)
         (REST (REQUEST "POST" template (vars...) .body) .rest))
      ((REST (PUT template (vars...) .body) .rest)
         (REST (REQUEST "PUT" template (vars...) .body) .rest))
      ((REST (PATCH template (vars...) .body) .rest)
         (REST (REQUEST "PATCH" template (vars...) .body) .rest))
      ((REST (DELETE template (vars...) .body) .rest)
         (REST (REQUEST "DELETE" template (vars...) .body) .rest))

      ((REST (clause exp . rest-exps) .rest) ; any other clause
         (if clause
            ((lambda () exp . rest-exps)) ; (begin ...)
            (REST . rest)))))

; --------------------------------------------------------------------
; sha1
(import (lib sha1))
(import (otus ffi))

; create extension function
(define sqlite3_context* type-vptr)
(define calculate (vm:pin (cons
   (cons fft-int (list
   ;  context          
      sqlite3_context* fft-int (list type-vptr)))
   (lambda (context argc argv)
      (define v (sqlite3_value_text (car argv)))
      (print "v: " v)
      (define r (base64:encode (sha1:digest v)))
      (sqlite3_result_text context r -1 #false))
)))
(define sha1-function (make-callback calculate))

; --------------
; цвета
(define RED "\e[0;31m")
(define GREEN "\e[0;32m")
(define YELLOW "\e[0;33m")
(define BLUE "\e[0;34m")
(define MAGENTA "\e[0;35m")
(define CYAN "\e[0;36m")
(define WHITE "\e[0;37m")
(define END "\e[0;0m")
(define (LOGD . args)
   (apply print-to (cons stderr args)))
; --------------
(define (neq? x q) (not (eq? x q)))

; -=( http run 6002 )=-------------------------------------
(import (lib http)
      (file json)
      (otus random!))

(http:run 5002 (lambda (fd request headers stream return)
   (LOGD "\nRequest: " BLUE request END)

   (define database (make-sqlite3)) ; make new sql database connection
   (sqlite3_open "database.sqlite" database)
   (db:query "PRAGMA foreign_keys = ON") ; enable foreign keys support

   (let*((al (ref request 2))   ; al - address line
         (pu (http:parse-url al)) ; pu - parsed url
         (path (ref pu 1))
         (args (ref pu 2))

         (content-type (headers 'content-type #f))
         (_ (print "content-type: " content-type))

         (body stream
            ; если пришел json - разберем его,
            ; иначе разберем строку запроса и не трогаем payload
            (if (and content-type (string-ci=? content-type "application/json"))
            then
               (let ((json (read-json stream)))
                  (print "json: " json)
                  (if json
                     (values json #false)
                     (values {} stream)))
            else
               (values {} stream)))
         ; пользовательская сессия
         (session (headers 'x-tmagps-sid #f)))

;  (print "body: " body)

   ; аналог функции write для сокета
   (define (send . args)
      (for-each (lambda (arg)
         (display-to fd arg)) args))

   ; ----------------------------------
   ; http/ response
   (define (respond color status . args)
      (LOGD color "Sending " status END)
      (send "HTTP/1.0 " status "\r\n")
      (send "Content-Type: text/html"        "\r\n"
            "Access-Control-Allow-Origin: *" "\r\n" ; TEMP, allow any thirdparty clients to play
            "Cache-Control: no-store"        "\r\n"
            "Server: " (car *version*) "/" (cdr *version*) "\r\n"
            "Connection: close"              "\r\n"
            "Content-Length: 0"              "\r\n"
            "\r\n")
      (apply send args)
      (sqlite3_close database)
      (return #true))

   ; http/ response codes
   (define (send-200) (respond GREEN "200 OK" ""))                (define send-ok send-200) ; ({} во избежание ошибки парсинга json)
   (define (send-204) (respond GREEN "204 No Content"))           (define send-no-content send-204)
   (define (send-400) (respond RED   "400 Bad Request"))          (define send-bad-request send-400)
   (define (send-401) (respond RED   "401 Unauthorized"))         (define send-unauthorized send-401)
   (define (send-403) (respond RED   "403 Forbidden"))            (define send-forbidden send-403)
   (define (send-404) (respond RED   "404 Not Found"))            (define send-not-found send-404)
   (define (send-405) (respond RED   "405 Method Not Allowed"))   (define send-method-not-allowed send-405)
   (define (send-422) (respond RED   "422 Unprocessable Entity")) (define send-unprocessable-entity send-422)
   (define (send-500) (respond RED   "500 Internal Server Error"))(define send-internal-server-error send-500)
   (define (send-501) (respond RED   "501 Not Implemented"))      (define send-not-implemented send-501)

   ; отправить json в сокет
   (define (send-json json)
      (define bytes (string->utf8 (stringify json)))

      (LOGD "Sending json (with 200 OK): " GREEN json END)
      (send "HTTP/1.0 " "200 OK" "\r\n")
      (send "Access-Control-Allow-Origin: *" "\r\n" ; TESTING, allow any thirdparty clients to play
            "Content-Type: application/json" "\r\n"
            "Content-Length: " (size bytes)  "\r\n"
            "Cache-Control: no-store"        "\r\n"
            "Server: " (car *version*) "/" (cdr *version*) "\r\n"
            "Connection: close"              "\r\n"
            "\r\n")
      (syscall 1 fd bytes)
      (sqlite3_close database)
      (return #true))

   ; ========================================================================
   ; ------------------------------------------------------------------------
   (REST
      ; -------------------------------------
      ; Cross-Domain Access Processor
      ((string-eq? (ref request 1) "OPTIONS")
         (LOGD "Sending " GREEN "200 OK" END)
         (send "HTTP/1.0 " "200 OK" "\r\n")
         (send "Access-Control-Allow-Origin: *"                           "\r\n"
               "Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE"  "\r\n"
               "Access-Control-Allow-Headers: Content-Type,X-TmaGps-SID" "\r\n"
               "Server: " (car *version*) "/" (cdr *version*)             "\r\n"
               "Connection: close"                                        "\r\n"
               "Content-Length: 0"                                        "\r\n"
               "\r\n")
         (sqlite3_close database) ; do not forget to close the database connection
         (return #true))

      ; Логин
      ; TODO: add sqlite extension function sha256
      ((and (string-eq? path "/tma/login")
            (string-eq? (ref request 1) "POST"))
         (let ((login    (getf body 'login))
               (password (getf body 'password))
               (remote_address (headers 'x-real-ip (car (syscall 51 fd)))))
            (sqlite3_create_function_v2 database "SHA1" 1 SQLITE_UTF8 #f sha1-function #f #f #f)
            (db:apply (db:value "
                  UPDATE accounts
                     SET session = lower(hex(randomblob(16))),
                           remote_address = ?3
                     WHERE login = ?1 AND password = SHA1('hf:'||?1||':'||?2||':'||LENGTH(?2))
               RETURNING enabled,name,session"  login password remote_address)
               (lambda  (enabled name session)
                  (case enabled
                     (0 (send-forbidden)) ; 403
                     (1 (send-json {      ; 200
                        'name name
                        'session session
                     })))))
            ; else
            (send-unauthorized))) ; 401

      ; иначе просто двигаемся дальше
      (else #f))
   ; ----------------------------------------------------------------------------
   ; весь остальной API требует авторизации
   (define account (db:value "SELECT id FROM accounts WHERE session = ?" session))
   (print "session, account: " session ", " account)
   (unless account
      (send-unauthorized))

   (REST
      ; | HTTP | POST   | GET    | PUT              | DELETE | PATCH  |
      ; | SQL  | INSERT | SELECT | UPDATE OR INSERT | DELETE | UPDATE |
      ; --------------------------------------------
      ; handle "ping" to check the session logged in
      ; todo: add "ping" counter to prevent password
      ;       bruteforcing
      (GET "/tma/ping" ()
         (send-ok)) ; 200

      ; logout
      (POST "/tma/logout" ()
         ; сбрасываем сессионный ключ
         (if (db:value "
               UPDATE accounts
                  SET session=NULL
                WHERE session=?" session)
            (send-ok))
         (send-internal-server-error))

      ;; ; return all locations in group
      ;; (GET "/tma/locations" () ; todo: add group id
      ;;    (send-json (list->vector
      ;;       (db:map (db:query "SELECT latitude, longitude
      ;;                            FROM locations
      ;;                         WHERE account=?" account)
      ;;          (lambda (lat lon) {
      ;;             'lat lat
      ;;             'lon lon
      ;;          }))))
      ;;    (send-404))

      ; record new user location
      (PUT "/tma/location" ()
         (define utc_time (getf body 'utc))
         (define latitude (getf body 'lat))
         (define longitude (getf body 'lon))
         (define course (getf body 'course))
         (define speed (getf body 'speed))

         (let ((id (db:value "
                  INSERT INTO locations (account,
                        latitude, longitude,
                        utc_time, course, speed) VALUES (?,?,?,?,?,?)
                  RETURNING id" account latitude longitude utc_time course speed)))
            (when (number? id)
               (send-json (list->vector (db:map (db:query "
                     SELECT account,
                            latitude, longitude,
                            course, speed, utc_time
                      FROM locations
                     WHERE latitude != 0
                       AND account != ?
                     GROUP BY account
                    HAVING MAX(id)" account)
                  (lambda (account latitude longitude course speed utc_time) {
                     'name "name"
                     'acc account
                     ; main info
                     'lat latitude
                     'lon longitude
                     ; additional info
                     'utc utc_time
                     'course course
                     'speed speed
                  })))))
            (send-500))
         (send-404))

      ;else
      (else
         (send-404))))))
