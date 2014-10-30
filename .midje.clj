(declare midje-junit-formatter.core)
(when (bound? #'midje-junit-formatter.core)
  (change-defaults :emitter 'midje-junit-formatter.core
                 :print-level :print-facts))