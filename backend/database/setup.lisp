#!/usr/bin/env ol

,load "setup/connect.lisp"
;,load "setup/private.lisp" ; донастройка под себя
;,load "setup/options.lisp"

; -----------------------------------------------
(print "Creating accounts")
,load "demo/users.lisp"
