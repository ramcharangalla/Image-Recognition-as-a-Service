#!/bin/bash
#!/home/ubuntu/tensorflow/bin/python
source /home/ubuntu/tensorflow/bin/activate
source ~/.bashrc
cd /home/ubuntu/tensorflow/models/tutorials/image/imagenet
python job.py > result.log 
