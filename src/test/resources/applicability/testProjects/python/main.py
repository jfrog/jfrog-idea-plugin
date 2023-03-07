#!/usr/bin/env python3
import os
import tarfile


def py_files(members):
    for tarinfo in members:
        if os.path.splitext(tarinfo.name)[1] == ".py":
            yield tarinfo


def get_name():
    return "sample.tar.gz"


name = get_name()
tar = tarfile.open(name)
tar.extractall(members=py_files(tar))
tar.close()
