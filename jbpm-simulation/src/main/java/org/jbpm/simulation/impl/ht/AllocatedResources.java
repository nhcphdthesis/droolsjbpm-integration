/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.jbpm.simulation.impl.ht;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AllocatedResources {

    private int poolSize;
    private long performedWork = 0;
    private List<Long> allocatedTill = new ArrayList<Long>();//should be replaced by allocatedIntervals
    
    public AllocatedResources(int poolSize) {
        this.poolSize = poolSize;
    }
    
    public AllocatedWork allocate(long startTime, long duration, long limit) {
        long waitTime = 0;
        AllocatedWork allocatedWork = new AllocatedWork(duration);
        performedWork += duration;
        System.out.println(String.format("in AllocatedResources.allocate(startTime:%d, duration:%d, limit:%d), current poolSize: %d, allocatedTill.size()=%d", 
        		startTime,duration,limit,poolSize,allocatedTill.size()));
        if(poolSize == 0) {
            // no available resources
            allocatedWork.setAllocatedTime(startTime + duration);
            allocatedWork.setWaitTime(duration);

            return allocatedWork;
        }
        
        if(allocatedTill.size() < poolSize) {
            long allocated = startTime + duration; 
            if (allocated > limit) {
                allocated = limit;
            }
            allocatedTill.add(allocated);
            System.out.println(String.format("resource available (argstill.size<poolSize)). allocated: %d, allocatedTime:%d, waitTime:%d",allocated,allocated-startTime,waitTime));
            //this is where to change the waiting time if we consider scheduling (not start the task immediately)
            //here waiting time means the waiting from the moment the task should be started to the moment the task can be actually started (waiting for resource)
            //if we consider scheduling, then this data structure (only keep the finishing time of resources) is not enough, because we are also interested in the starting time of tasks 
            //(ie., we move from finishing points to allocated intervals)
            allocatedWork.setAllocatedTime(allocated - startTime);
            allocatedWork.setWaitTime(waitTime);
         } else {
             Collections.sort(allocatedTill);
        
             long allocated = allocatedTill.get(0);//get the soonest allocated one
             System.out.println(String.format("allocatedTill.get(0) [ie.allocated]: %d, limit:%d", allocated,limit));
             if (allocated == limit) {
            	 System.out.println("allocated == limit, now waiting time = allocated-startTime, wait for the soonest finishing one");
                 waitTime = allocated - startTime;
                 allocatedWork.setAllocatedTime(0);
                 allocatedWork.setWaitTime(waitTime);
                 
                 return allocatedWork;
             }
             
             //should add case when already allocated timeslot falls within the [scheduled start time, finish time] of the current task. Because the finishing time of this scheduled task affects the possible start time of already planned other tasks
             if (allocated >= startTime) {
            	 System.out.println("allocated >= startTime, now waiting time = allocated-startTime");
                 waitTime = allocated - startTime;
                 allocated += duration;
        
             } else {
            	 System.out.println("allocated < startTime, no waiting time");
                 allocated = startTime + duration;
             }
             if (allocated > limit) {
            	 System.out.println("allocated>limit");
                 allocatedTill.set(0, limit);
                 allocatedWork.setAllocatedTime(duration - (allocated - limit));
                 allocatedWork.setWaitTime(waitTime);
             } else {
            	 System.out.println("allocated<=limit");
                 allocatedTill.set(0, allocated);
                 
                 allocatedWork.setAllocatedTime(allocated - startTime);
                 allocatedWork.setWaitTime(waitTime);
             }
             
        }
        
        return allocatedWork;
    }
}
