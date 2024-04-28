#!/usr/bin/ol

; http://stars.arglos.net/
; https://www.gamefaqs.com/pc/198797-stars/faqs/41043

;(define *features* (cons 'sqlite-log-debug (features))) ; enable debug/
(import (lib sqlite))

(define database (make-sqlite3)) ; make new database connection
(sqlite3_open "../database.sqlite" database)

(define (db:value . args) (apply sqlite:value (cons database args)))
(define (db:query . args) (apply sqlite:query (cons database args)))

; https://www.sqlite.org/foreignkeys.html
; todo: think and apply to foreign keys the RESTRICT or CASCADE clause!
(db:query "PRAGMA foreign_keys = ON")
