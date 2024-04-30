#!/usr/bin/env ol

(import (otus random!))
(define (select . args)
   (list-ref args (rand! (length args))))

(define (random3) (string (select #\0 #\1 #\2)))
(define (random4) (string (select #\0 #\1 #\2 #\3)))
(define (Gagarin) (string (select #\G #\a #\g #\a #\r #\i #\n)))
(define (Galileo) (string (select #\G #\a #\l #\i #\l #\e #\o)))

(define (concatenate . args)
   (define strings (map (lambda (arg)
      (cond
         ((string? arg) arg)
         ((number? arg)
            (number->string arg))
         (else #false))) args))
   (apply string-append strings))
