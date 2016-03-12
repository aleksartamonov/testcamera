# testcamera

Алгоритм

1) Загружается картинка
2) Переводится в градацию серого и ищутся KeyPoints с помощью Brisk
3) Извлекаются дескрипторы(получается 6 фич с KeyPoint (angle, octave, size, ptx, pty,response)+ 64 из дескриптора
4) Создаю dataset размера batchSize(иначе будет OutOfMemory, если все сразу давать)
5) Запускаю svm на этой точке.
