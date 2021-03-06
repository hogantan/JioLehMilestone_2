package com.example.jioleh.favourites;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.jioleh.R;
import com.example.jioleh.listings.ActivityAdapter;
import com.example.jioleh.listings.JioActivity;
import com.example.jioleh.listings.ViewParticipantsAdapter;
import com.example.jioleh.userprofile.UserProfile;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class JoinedFragment extends Fragment {

    private View currentView;
    private TextView emptyText;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private FavouritesAdapter adapter;

    private FirebaseUser currentUser;
    private FirebaseFirestore datastore;
    private ArrayList<JioActivity> list_of_activities = new ArrayList<>();
    private ArrayList<Task<DocumentSnapshot>> list_of_tasks = new ArrayList<>();


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        currentView = inflater.inflate(R.layout.fragment_joined, container, false);
        initialise();
        initialiseRecyclerView();
        checkActivityExpiry();
        checkActivityCancelledConfirmed();
        getJoined();

        //this is to update when an activity expires but it does not get reflected since join and like fragment does not listen to field data of activity
        //Third line of check
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                //Second line of check
                checkActivityExpiry();
                checkActivityCancelledConfirmed();
                getJoined();
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        return currentView;
    }

    private void initialise() {
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        datastore = FirebaseFirestore.getInstance();
        swipeRefreshLayout = currentView.findViewById(R.id.swipeContainer);
        emptyText = currentView.findViewById(R.id.tvFavouriteEmpty);
    }

    //To locate the activities that the user has joined
    private void getJoined() {
        datastore.collection("users")
                .document(currentUser.getUid())
                .collection("joined")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                        //listens to joined for changes which will only occur if there is a change in activities collection which is prompted by ViewJioActivity Listener
                        List<DocumentSnapshot> snapshots = queryDocumentSnapshots.getDocuments();

                        list_of_tasks.clear();
                        list_of_activities.clear();

                        //Adding a list of completable futures
                        for (DocumentSnapshot documentSnapshot : snapshots) {
                            list_of_tasks.add(getActivity(documentSnapshot.getId()));
                        }

                        //Waiting for completable futures to complete
                        Tasks.whenAllSuccess(list_of_tasks).addOnSuccessListener(new OnSuccessListener<List<? super DocumentSnapshot>>() {
                            @Override
                            public void onSuccess(List<? super DocumentSnapshot> snapShots) {
                                //to show activities that are not expired first
                                //linear time sorting though, although doubt that n will become too large anyways
                                Collections.sort(list_of_activities, new Comparator<JioActivity>() {
                                    @Override
                                    public int compare(JioActivity o1, JioActivity o2) {
                                        //Arranges activities based on actual event date and time to show user the most upcoming events
                                        return o1.getEvent_timestamp().compareTo(o2.getEvent_timestamp());
                                    }
                                });
                                adapter.setData(list_of_activities, false, true);
                                adapter.notifyDataSetChanged();
                                //Visual text
                                if (list_of_activities.isEmpty()) {
                                    emptyText.setText("You have not join any activities!");
                                } else {
                                    emptyText.setText("");
                                }
                            }
                        });
                    }
                });
    }

    //Get a completable future task
    private Task<DocumentSnapshot> getActivity(final String uid) {
        return datastore.collection("activities")
                .document(uid)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            list_of_activities.add(documentSnapshot.toObject(JioActivity.class));
                        } else {
                            //removes from current user joined activities
                            datastore.collection("users")
                                    .document(currentUser.getUid())
                                    .collection("joined")
                                    .document(uid)
                                    .delete();
                        }
                    }
                });
    }

    private void initialiseRecyclerView() {
        adapter = new FavouritesAdapter();
        recyclerView = currentView.findViewById(R.id.rvFavouriteJoined);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        recyclerView.setAdapter(adapter);
    }

    public void checkActivityExpiry() {
        Date currentDateTime = Calendar.getInstance().getTime(); //this gets both date and time
        CollectionReference jioActivityColRef = FirebaseFirestore.getInstance().collection("activities");

        jioActivityColRef.whereLessThan("event_timestamp", currentDateTime)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        List<DocumentSnapshot> list_of_documents = queryDocumentSnapshots.getDocuments();
                        for (DocumentSnapshot documentSnapshot: list_of_documents) {
                            jioActivityColRef.document(documentSnapshot.getId())
                                    .update("expired", true);
                        }
                    }
                });
    }

    public void checkActivityCancelledConfirmed() {
        Date currentDateTime = Calendar.getInstance().getTime(); //this gets both date and time
        CollectionReference jioActivityColRef = FirebaseFirestore.getInstance().collection("activities");

        jioActivityColRef.whereLessThan("deadline_timestamp", currentDateTime)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        List<DocumentSnapshot> list_of_documents = queryDocumentSnapshots.getDocuments();
                        for (DocumentSnapshot documentSnapshot: list_of_documents) {
                            int minimum = Integer.parseInt(documentSnapshot.get("min_participants").toString());
                            int current = Integer.parseInt(documentSnapshot.get("current_participants").toString());
                            if (current < minimum) {
                                jioActivityColRef.document(documentSnapshot.getId())
                                        .update("cancelled", true);
                            } else {
                                jioActivityColRef.document(documentSnapshot.getId())
                                        .update("confirmed", true);
                            }
                        }
                    }
                });
    }
}