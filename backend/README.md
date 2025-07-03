TMA backend
===========

Prerequisites
-------------

* Sqlite 3
  `apt install libsqlite3`
* Otus Lisp
  `git clone https://github.com/otus-lisp/ol; cd ol; make; sudo make install`

Setup
-----
Run `./make` script to create `database.sqlite`.

Running
-------
Run `./backend.lisp` to run backend. It'll start listening on port 5002 (which you can change in code).

Open 'http://127.0.0.1:5002/tma#someroom' url in the any browser (change 'someroom' to match your room).
